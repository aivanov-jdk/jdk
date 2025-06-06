/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021, Red Hat, Inc. and/or its affiliates.
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "classfile/javaClasses.hpp"
#include "gc/shared/workerThread.hpp"
#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahGeneration.hpp"
#include "gc/shenandoah/shenandoahReferenceProcessor.hpp"
#include "gc/shenandoah/shenandoahScanRemembered.inline.hpp"
#include "gc/shenandoah/shenandoahThreadLocalData.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"

static ReferenceType reference_type(oop reference) {
  return InstanceKlass::cast(reference->klass())->reference_type();
}

static const char* reference_type_name(ReferenceType type) {
  switch (type) {
    case REF_SOFT:
      return "Soft";

    case REF_WEAK:
      return "Weak";

    case REF_FINAL:
      return "Final";

    case REF_PHANTOM:
      return "Phantom";

    default:
      ShouldNotReachHere();
      return nullptr;
  }
}

template <typename T>
static void card_mark_barrier(T* field, oop value) {
  assert(ShenandoahCardBarrier, "Card-mark barrier should be on");
  ShenandoahGenerationalHeap* heap = ShenandoahGenerationalHeap::heap();
  assert(heap->is_in_or_null(value), "Should be in heap");
  if (heap->is_in_old(field) && heap->is_in_young(value)) {
    // For Shenandoah, each generation collects all the _referents_ that belong to the
    // collected generation. We can end up with discovered lists that contain a mixture
    // of old and young _references_. These references are linked together through the
    // discovered field in java.lang.Reference. In some cases, creating or editing this
    // list may result in the creation of _new_ old-to-young pointers which must dirty
    // the corresponding card. Failing to do this may cause heap verification errors and
    // lead to incorrect GC behavior.
    heap->old_generation()->mark_card_as_dirty(field);
  }
}

template <typename T>
static void set_oop_field(T* field, oop value);

template <>
void set_oop_field<oop>(oop* field, oop value) {
  *field = value;
  if (ShenandoahCardBarrier) {
    card_mark_barrier(field, value);
  }
}

template <>
void set_oop_field<narrowOop>(narrowOop* field, oop value) {
  *field = CompressedOops::encode(value);
  if (ShenandoahCardBarrier) {
    card_mark_barrier(field, value);
  }
}

static oop lrb(oop obj) {
  if (obj != nullptr && ShenandoahHeap::heap()->marking_context()->is_marked(obj)) {
    return ShenandoahBarrierSet::barrier_set()->load_reference_barrier(obj);
  } else {
    return obj;
  }
}

template <typename T>
static volatile T* reference_referent_addr(oop reference) {
  return (volatile T*)java_lang_ref_Reference::referent_addr_raw(reference);
}

inline oop reference_coop_decode_raw(narrowOop v) {
  return CompressedOops::is_null(v) ? nullptr : CompressedOops::decode_raw(v);
}

inline oop reference_coop_decode_raw(oop v) {
  return v;
}

// Raw referent, it can be dead. You cannot treat it as oop without additional safety
// checks, this is why it is HeapWord*. The decoding uses a special-case inlined
// CompressedOops::decode method that bypasses normal oop-ness checks.
template <typename T>
static HeapWord* reference_referent_raw(oop reference) {
  T raw_oop = Atomic::load(reference_referent_addr<T>(reference));
  return cast_from_oop<HeapWord*>(reference_coop_decode_raw(raw_oop));
}

static void reference_clear_referent(oop reference) {
  java_lang_ref_Reference::clear_referent_raw(reference);
}

template <typename T>
static T* reference_discovered_addr(oop reference) {
  return reinterpret_cast<T*>(java_lang_ref_Reference::discovered_addr_raw(reference));
}

template <typename T>
static oop reference_discovered(oop reference) {
  T heap_oop = *reference_discovered_addr<T>(reference);
  return lrb(CompressedOops::decode(heap_oop));
}

template <typename T>
static void reference_set_discovered(oop reference, oop discovered);

template <>
void reference_set_discovered<oop>(oop reference, oop discovered) {
  *reference_discovered_addr<oop>(reference) = discovered;
}

template <>
void reference_set_discovered<narrowOop>(oop reference, oop discovered) {
  *reference_discovered_addr<narrowOop>(reference) = CompressedOops::encode(discovered);
}

template<typename T>
static bool reference_cas_discovered(oop reference, oop discovered) {
  T* addr = reinterpret_cast<T *>(java_lang_ref_Reference::discovered_addr_raw(reference));
  return ShenandoahHeap::atomic_update_oop_check(discovered, addr, nullptr);
}

template <typename T>
static T* reference_next_addr(oop reference) {
  return reinterpret_cast<T*>(java_lang_ref_Reference::next_addr_raw(reference));
}

template <typename T>
static oop reference_next(oop reference) {
  T heap_oop = RawAccess<>::oop_load(reference_next_addr<T>(reference));
  return lrb(CompressedOops::decode(heap_oop));
}

static void reference_set_next(oop reference, oop next) {
  java_lang_ref_Reference::set_next_raw(reference, next);
}

static void soft_reference_update_clock() {
  const jlong now = os::javaTimeNanos() / NANOSECS_PER_MILLISEC;
  java_lang_ref_SoftReference::set_clock(now);
}

ShenandoahRefProcThreadLocal::ShenandoahRefProcThreadLocal() :
  _discovered_list(nullptr),
  _encountered_count(),
  _discovered_count(),
  _enqueued_count() {
}

void ShenandoahRefProcThreadLocal::reset() {
  _discovered_list = nullptr;
  _mark_closure = nullptr;
  for (uint i = 0; i < reference_type_count; i++) {
    _encountered_count[i] = 0;
    _discovered_count[i] = 0;
    _enqueued_count[i] = 0;
  }
}

template <typename T>
T* ShenandoahRefProcThreadLocal::discovered_list_addr() {
  return reinterpret_cast<T*>(&_discovered_list);
}

template <>
oop ShenandoahRefProcThreadLocal::discovered_list_head<oop>() const {
  return *reinterpret_cast<const oop*>(&_discovered_list);
}

template <>
oop ShenandoahRefProcThreadLocal::discovered_list_head<narrowOop>() const {
  return CompressedOops::decode(*reinterpret_cast<const narrowOop*>(&_discovered_list));
}

template <>
void ShenandoahRefProcThreadLocal::set_discovered_list_head<narrowOop>(oop head) {
  *discovered_list_addr<narrowOop>() = CompressedOops::encode(head);
}

template <>
void ShenandoahRefProcThreadLocal::set_discovered_list_head<oop>(oop head) {
  *discovered_list_addr<oop>() = head;
}

ShenandoahReferenceProcessor::ShenandoahReferenceProcessor(uint max_workers) :
  _soft_reference_policy(nullptr),
  _ref_proc_thread_locals(NEW_C_HEAP_ARRAY(ShenandoahRefProcThreadLocal, max_workers, mtGC)),
  _pending_list(nullptr),
  _pending_list_tail(&_pending_list),
  _iterate_discovered_list_id(0U),
  _stats() {
  for (size_t i = 0; i < max_workers; i++) {
    _ref_proc_thread_locals[i].reset();
  }
}

void ShenandoahReferenceProcessor::reset_thread_locals() {
  uint max_workers = ShenandoahHeap::heap()->max_workers();
  for (uint i = 0; i < max_workers; i++) {
    _ref_proc_thread_locals[i].reset();
  }
}

void ShenandoahReferenceProcessor::set_mark_closure(uint worker_id, ShenandoahMarkRefsSuperClosure* mark_closure) {
  _ref_proc_thread_locals[worker_id].set_mark_closure(mark_closure);
}

void ShenandoahReferenceProcessor::set_soft_reference_policy(bool clear) {
  static AlwaysClearPolicy always_clear_policy;
  static LRUMaxHeapPolicy lru_max_heap_policy;

  if (clear) {
    log_info(gc, ref)("Clearing All SoftReferences");
    _soft_reference_policy = &always_clear_policy;
  } else {
    _soft_reference_policy = &lru_max_heap_policy;
  }

  _soft_reference_policy->setup();
}

template <typename T>
bool ShenandoahReferenceProcessor::is_inactive(oop reference, oop referent, ReferenceType type) const {
  if (type == REF_FINAL) {
    // A FinalReference is inactive if its next field is non-null. An application can't
    // call enqueue() or clear() on a FinalReference.
    return reference_next<T>(reference) != nullptr;
  } else {
    // A non-FinalReference is inactive if the referent is null. The referent can only
    // be null if the application called Reference.enqueue() or Reference.clear().
    return referent == nullptr;
  }
}

bool ShenandoahReferenceProcessor::is_strongly_live(oop referent) const {
  return ShenandoahHeap::heap()->marking_context()->is_marked_strong(referent);
}

bool ShenandoahReferenceProcessor::is_softly_live(oop reference, ReferenceType type) const {
  if (type != REF_SOFT) {
    // Not a SoftReference
    return false;
  }

  // Ask SoftReference policy
  const jlong clock = java_lang_ref_SoftReference::clock();
  assert(clock != 0, "Clock not initialized");
  assert(_soft_reference_policy != nullptr, "Policy not initialized");
  return !_soft_reference_policy->should_clear_reference(reference, clock);
}

template <typename T>
bool ShenandoahReferenceProcessor::should_discover(oop reference, ReferenceType type) const {
  T* referent_addr = (T*) java_lang_ref_Reference::referent_addr_raw(reference);
  T heap_oop = RawAccess<>::oop_load(referent_addr);
  oop referent = CompressedOops::decode(heap_oop);
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (is_inactive<T>(reference, referent, type)) {
    log_trace(gc,ref)("Reference inactive: " PTR_FORMAT, p2i(reference));
    return false;
  }

  if (is_strongly_live(referent)) {
    log_trace(gc,ref)("Reference strongly live: " PTR_FORMAT, p2i(reference));
    return false;
  }

  if (is_softly_live(reference, type)) {
    log_trace(gc,ref)("Reference softly live: " PTR_FORMAT, p2i(reference));
    return false;
  }

  if (!heap->is_in_active_generation(referent)) {
    log_trace(gc,ref)("Referent outside of active generation: " PTR_FORMAT, p2i(referent));
    return false;
  }

  return true;
}

template <typename T>
bool ShenandoahReferenceProcessor::should_drop(oop reference, ReferenceType type) const {
  HeapWord* raw_referent = reference_referent_raw<T>(reference);
  if (raw_referent == nullptr) {
    // Reference has been cleared, by a call to Reference.enqueue()
    // or Reference.clear() from the application, which means we
    // should drop the reference.
    return true;
  }

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  // Check if the referent is still alive, in which case we should
  // drop the reference.
  if (type == REF_PHANTOM) {
    return heap->active_generation()->complete_marking_context()->is_marked(raw_referent);
  } else {
    return heap->active_generation()->complete_marking_context()->is_marked_strong(raw_referent);
  }
}

template <typename T>
void ShenandoahReferenceProcessor::make_inactive(oop reference, ReferenceType type) const {
  if (type == REF_FINAL) {
    // Don't clear referent. It is needed by the Finalizer thread to make the call
    // to finalize(). A FinalReference is instead made inactive by self-looping the
    // next field. An application can't call FinalReference.enqueue(), so there is
    // no race to worry about when setting the next field.
    assert(reference_next<T>(reference) == nullptr, "Already inactive");
    assert(ShenandoahHeap::heap()->active_generation()->complete_marking_context()->is_marked(reference_referent_raw<T>(reference)), "only make inactive final refs with alive referents");
    reference_set_next(reference, reference);
  } else {
    // Clear referent
    reference_clear_referent(reference);
  }
}

template <typename T>
bool ShenandoahReferenceProcessor::discover(oop reference, ReferenceType type, uint worker_id) {
  if (!should_discover<T>(reference, type)) {
    // Not discovered
    return false;
  }

  if (reference_discovered<T>(reference) != nullptr) {
    // Already discovered. This can happen if the reference is marked finalizable first, and then strong,
    // in which case it will be seen 2x by marking.
    log_trace(gc,ref)("Reference already discovered: " PTR_FORMAT, p2i(reference));
    return true;
  }

  if (type == REF_FINAL) {
    ShenandoahMarkRefsSuperClosure* cl = _ref_proc_thread_locals[worker_id].mark_closure();
    bool weak = cl->is_weak();
    cl->set_weak(true);
    if (UseCompressedOops) {
      cl->do_oop(reinterpret_cast<narrowOop*>(java_lang_ref_Reference::referent_addr_raw(reference)));
    } else {
      cl->do_oop(reinterpret_cast<oop*>(java_lang_ref_Reference::referent_addr_raw(reference)));
    }
    cl->set_weak(weak);
  }

  // Add reference to discovered list
  // Each worker thread has a private copy of refproc_data, which includes a private discovered list.  This means
  // there's no risk that a different worker thread will try to manipulate my discovered list head while I'm making
  // reference the head of my discovered list.
  ShenandoahRefProcThreadLocal& refproc_data = _ref_proc_thread_locals[worker_id];
  oop discovered_head = refproc_data.discovered_list_head<T>();
  if (discovered_head == nullptr) {
    // Self-loop tail of list. We distinguish discovered from not-discovered references by looking at their
    // discovered field: if it is null, then it is not-yet discovered, otherwise it is discovered
    discovered_head = reference;
  }
  if (reference_cas_discovered<T>(reference, discovered_head)) {
    // We successfully set this reference object's next pointer to discovered_head.  This marks reference as discovered.
    // If reference_cas_discovered fails, that means some other worker thread took credit for discovery of this reference,
    // and that other thread will place reference on its discovered list, so I can ignore reference.

    // In case we have created an interesting pointer, mark the remembered set card as dirty.
    if (ShenandoahCardBarrier) {
      T* addr = reinterpret_cast<T*>(java_lang_ref_Reference::discovered_addr_raw(reference));
      card_mark_barrier(addr, discovered_head);
    }

    // Make the discovered_list_head point to reference.
    refproc_data.set_discovered_list_head<T>(reference);
    assert(refproc_data.discovered_list_head<T>() == reference, "reference must be new discovered head");
    log_trace(gc, ref)("Discovered Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));
    _ref_proc_thread_locals[worker_id].inc_discovered(type);
  }
  return true;
}

bool ShenandoahReferenceProcessor::discover_reference(oop reference, ReferenceType type) {
  if (!RegisterReferences) {
    // Reference processing disabled
    return false;
  }

  log_trace(gc, ref)("Encountered Reference: " PTR_FORMAT " (%s, %s)",
          p2i(reference), reference_type_name(type), ShenandoahHeap::heap()->heap_region_containing(reference)->affiliation_name());
  uint worker_id = WorkerThread::worker_id();
  _ref_proc_thread_locals[worker_id].inc_encountered(type);

  if (UseCompressedOops) {
    return discover<narrowOop>(reference, type, worker_id);
  } else {
    return discover<oop>(reference, type, worker_id);
  }
}

template <typename T>
oop ShenandoahReferenceProcessor::drop(oop reference, ReferenceType type) {
  log_trace(gc, ref)("Dropped Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));

  HeapWord* raw_referent = reference_referent_raw<T>(reference);

#ifdef ASSERT
  assert(raw_referent == nullptr || ShenandoahHeap::heap()->active_generation()->complete_marking_context()->is_marked(raw_referent),
         "only drop references with alive referents");
#endif

  // Unlink and return next in list
  oop next = reference_discovered<T>(reference);
  reference_set_discovered<T>(reference, nullptr);
  // When this reference was discovered, it would not have been marked. If it ends up surviving
  // the cycle, we need to dirty the card if the reference is old and the referent is young.  Note
  // that if the reference is not dropped, then its pointer to the referent will be nulled before
  // evacuation begins so card does not need to be dirtied.
  if (ShenandoahCardBarrier) {
    card_mark_barrier(cast_from_oop<HeapWord*>(reference), cast_to_oop(raw_referent));
  }
  return next;
}

template <typename T>
T* ShenandoahReferenceProcessor::keep(oop reference, ReferenceType type, uint worker_id) {
  log_trace(gc, ref)("Enqueued Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));

  // Update statistics
  _ref_proc_thread_locals[worker_id].inc_enqueued(type);

  // Make reference inactive
  make_inactive<T>(reference, type);

  // Return next in list
  return reference_discovered_addr<T>(reference);
}

template <typename T>
void ShenandoahReferenceProcessor::process_references(ShenandoahRefProcThreadLocal& refproc_data, uint worker_id) {
  log_trace(gc, ref)("Processing discovered list #%u : " PTR_FORMAT, worker_id, p2i(refproc_data.discovered_list_head<T>()));
  T* list = refproc_data.discovered_list_addr<T>();
  // The list head is basically a GC root, we need to resolve and update it,
  // otherwise we will later swap a from-space ref into Universe::pending_list().
  if (!CompressedOops::is_null(*list)) {
    oop first_resolved = lrb(CompressedOops::decode_not_null(*list));
    set_oop_field(list, first_resolved);
  }
  T* p = list;
  while (true) {
    const oop reference = lrb(CompressedOops::decode(*p));
    if (reference == nullptr) {
      break;
    }
    log_trace(gc, ref)("Processing reference: " PTR_FORMAT, p2i(reference));
    const ReferenceType type = reference_type(reference);

    if (should_drop<T>(reference, type)) {
      set_oop_field(p, drop<T>(reference, type));
    } else {
      p = keep<T>(reference, type, worker_id);
    }

    const oop discovered = lrb(reference_discovered<T>(reference));
    if (reference == discovered) {
      // Reset terminating self-loop to null
      reference_set_discovered<T>(reference, oop(nullptr));
      break;
    }
  }

  // Prepend discovered references to internal pending list
  // set_oop_field maintains the card mark barrier as this list is constructed.
  if (!CompressedOops::is_null(*list)) {
    oop head = lrb(CompressedOops::decode_not_null(*list));
    shenandoah_assert_not_in_cset_except(&head, head, ShenandoahHeap::heap()->cancelled_gc() || !ShenandoahLoadRefBarrier);
    oop prev = Atomic::xchg(&_pending_list, head);
    set_oop_field(p, prev);
    if (prev == nullptr) {
      // First to prepend to list, record tail
      _pending_list_tail = reinterpret_cast<void*>(p);
    }

    // Clear discovered list
    set_oop_field(list, oop(nullptr));
  }
}

void ShenandoahReferenceProcessor::work() {
  // Process discovered references
  uint max_workers = ShenandoahHeap::heap()->max_workers();
  uint worker_id = Atomic::add(&_iterate_discovered_list_id, 1U, memory_order_relaxed) - 1;
  while (worker_id < max_workers) {
    if (UseCompressedOops) {
      process_references<narrowOop>(_ref_proc_thread_locals[worker_id], worker_id);
    } else {
      process_references<oop>(_ref_proc_thread_locals[worker_id], worker_id);
    }
    worker_id = Atomic::add(&_iterate_discovered_list_id, 1U, memory_order_relaxed) - 1;
  }
}

class ShenandoahReferenceProcessorTask : public WorkerTask {
private:
  bool const                          _concurrent;
  ShenandoahPhaseTimings::Phase const _phase;
  ShenandoahReferenceProcessor* const _reference_processor;

public:
  ShenandoahReferenceProcessorTask(ShenandoahPhaseTimings::Phase phase, bool concurrent, ShenandoahReferenceProcessor* reference_processor) :
    WorkerTask("ShenandoahReferenceProcessorTask"),
    _concurrent(concurrent),
    _phase(phase),
    _reference_processor(reference_processor) {
  }

  virtual void work(uint worker_id) {
    if (_concurrent) {
      ShenandoahConcurrentWorkerSession worker_session(worker_id);
      ShenandoahWorkerTimingsTracker x(_phase, ShenandoahPhaseTimings::WeakRefProc, worker_id);
      _reference_processor->work();
    } else {
      ShenandoahParallelWorkerSession worker_session(worker_id);
      ShenandoahWorkerTimingsTracker x(_phase, ShenandoahPhaseTimings::WeakRefProc, worker_id);
      _reference_processor->work();
    }
  }
};

void ShenandoahReferenceProcessor::process_references(ShenandoahPhaseTimings::Phase phase, WorkerThreads* workers, bool concurrent) {

  Atomic::release_store_fence(&_iterate_discovered_list_id, 0U);

  // Process discovered lists
  ShenandoahReferenceProcessorTask task(phase, concurrent, this);
  workers->run_task(&task);

  // Update SoftReference clock
  soft_reference_update_clock();

  // Collect, log and trace statistics
  collect_statistics();

  enqueue_references(concurrent);
}

void ShenandoahReferenceProcessor::enqueue_references_locked() {
  // Prepend internal pending list to external pending list
  shenandoah_assert_not_in_cset_except(&_pending_list, _pending_list, ShenandoahHeap::heap()->cancelled_gc() || !ShenandoahLoadRefBarrier);

  // During reference processing, we maintain a local list of references that are identified by
  //   _pending_list and _pending_list_tail.  _pending_list_tail points to the next field of the last Reference object on
  //   the local list.
  //
  // There is also a global list of reference identified by Universe::_reference_pending_list

  // The following code has the effect of:
  //  1. Making the global Universe::_reference_pending_list point to my local list
  //  2. Overwriting the next field of the last Reference on my local list to point at the previous head of the
  //     global Universe::_reference_pending_list

  oop former_head_of_global_list = Universe::swap_reference_pending_list(_pending_list);
  if (UseCompressedOops) {
    set_oop_field<narrowOop>(reinterpret_cast<narrowOop*>(_pending_list_tail), former_head_of_global_list);
  } else {
    set_oop_field<oop>(reinterpret_cast<oop*>(_pending_list_tail), former_head_of_global_list);
  }
}

void ShenandoahReferenceProcessor::enqueue_references(bool concurrent) {
  if (_pending_list == nullptr) {
    // Nothing to enqueue
    return;
  }
  if (!concurrent) {
    // When called from mark-compact or degen-GC, the locking is done by the VMOperation,
    enqueue_references_locked();
  } else {
    // Heap_lock protects external pending list
    MonitorLocker ml(Heap_lock);

    enqueue_references_locked();

    // Notify ReferenceHandler thread
    ml.notify_all();
  }

  // Reset internal pending list
  _pending_list = nullptr;
  _pending_list_tail = &_pending_list;
}

template<typename T>
void ShenandoahReferenceProcessor::clean_discovered_list(T* list) {
  T discovered = *list;
  while (!CompressedOops::is_null(discovered)) {
    oop discovered_ref = CompressedOops::decode_not_null(discovered);
    set_oop_field<T>(list, oop(nullptr));
    list = reference_discovered_addr<T>(discovered_ref);
    discovered = *list;
  }
}

void ShenandoahReferenceProcessor::abandon_partial_discovery() {
  uint max_workers = ShenandoahHeap::heap()->max_workers();
  for (uint index = 0; index < max_workers; index++) {
    if (UseCompressedOops) {
      clean_discovered_list<narrowOop>(_ref_proc_thread_locals[index].discovered_list_addr<narrowOop>());
    } else {
      clean_discovered_list<oop>(_ref_proc_thread_locals[index].discovered_list_addr<oop>());
    }
  }
  if (_pending_list != nullptr) {
    oop pending = _pending_list;
    _pending_list = nullptr;
    if (UseCompressedOops) {
      narrowOop* list = reference_discovered_addr<narrowOop>(pending);
      clean_discovered_list<narrowOop>(list);
    } else {
      oop* list = reference_discovered_addr<oop>(pending);
      clean_discovered_list<oop>(list);
    }
  }
  _pending_list_tail = &_pending_list;
}

void ShenandoahReferenceProcessor::collect_statistics() {
  Counters encountered = {};
  Counters discovered = {};
  Counters enqueued = {};
  uint max_workers = ShenandoahHeap::heap()->max_workers();
  for (uint i = 0; i < max_workers; i++) {
    for (size_t type = 0; type < reference_type_count; type++) {
      encountered[type] += _ref_proc_thread_locals[i].encountered((ReferenceType)type);
      discovered[type] += _ref_proc_thread_locals[i].discovered((ReferenceType)type);
      enqueued[type] += _ref_proc_thread_locals[i].enqueued((ReferenceType)type);
    }
  }

  _stats = ReferenceProcessorStats(discovered[REF_SOFT],
                                   discovered[REF_WEAK],
                                   discovered[REF_FINAL],
                                   discovered[REF_PHANTOM]);

  log_info(gc,ref)("Encountered references: Soft: %zu, Weak: %zu, Final: %zu, Phantom: %zu",
                   encountered[REF_SOFT], encountered[REF_WEAK], encountered[REF_FINAL], encountered[REF_PHANTOM]);
  log_info(gc,ref)("Discovered  references: Soft: %zu, Weak: %zu, Final: %zu, Phantom: %zu",
                   discovered[REF_SOFT], discovered[REF_WEAK], discovered[REF_FINAL], discovered[REF_PHANTOM]);
  log_info(gc,ref)("Enqueued    references: Soft: %zu, Weak: %zu, Final: %zu, Phantom: %zu",
                   enqueued[REF_SOFT], enqueued[REF_WEAK], enqueued[REF_FINAL], enqueued[REF_PHANTOM]);
}
