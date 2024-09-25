public class RightOneColumnCentered {
    public static void main(String[] args) throws Exception {
        PassFailJFrame.builder()
                      .instructions(INSTRUCTIONS)
                      .rows(15)
                      .columns(30)
                      .testUI(() -> WindowCreator.createTestWindows(5))
                      .positionTestUI(PassFailJFrame.WindowLayouts::rightOneColumnCentered)
                      .build()
                      .awaitAndCheck();
    }

    private static final String INSTRUCTIONS = """
            A simple demo with 5 test windows positioned to
            the right of the instruction frame in one column.
            The column of the windows is centered vertically
            on the screen.
            
            Layout: WindowLayouts::rightOneColumnCentered
            """;
}
