import javax.swing.*;
import java.awt.*;
//import javax.swing.border.LineBorder;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

public class Tetris extends JFrame implements KeyListener{
    static GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
    private final static Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private final static int SCREEN_W = (int)SCREEN_SIZE.getWidth();
    private final static int SCREEN_H = (int)SCREEN_SIZE.getHeight();
    private final static int FRAME_W = 1012;
    private final static int FRAME_H = 562;
    private final static String OS = System.getProperty("os.name");
    private final static  Color[][] grid = new Color[20][10]; //placed blocks
    private final static  Color[][] activeGrid = new Color[23][10]; //unplaced blocks
    private static boolean gameOver;
    private static boolean isPaused;
    private static int linesCompleted = 0;
    private static long cycle;
    private final static int frameRate = 60;
    private static int framesElapsed = 0;
    private static final String[] queue = {"line", "miniT", "leftL", "rightL", "leftZ", "rightZ", "cube"};
    private static int[][] blockLocation;
    private static Color blockColor;
    private static String blockType;
    private static boolean stall = false;
    private static int stallFrame = 0;

    private final static Set<Integer> pressedKeys = new HashSet<>();
    private final static Set<Integer> handledKeys = new HashSet<>();

    private static JLabel scoreLabel;

    private final static JPanel gameOverPanel = new JPanel();

    private static final JPanel gamePanel = new JPanel() {
    @Override 
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int gridNum = 50;
        int interval = getWidth() / gridNum;

        // Dimensions of the game area in grid cells
        int gridCols = 10; // Number of columns in the playable grid
        int gridRows = 20; // Number of rows in the playable grid
        int borderPadding = 1; // Padding around the grid inside the panel

        int blockAreaCols = gridCols + 2 * borderPadding;
        int blockAreaRows = gridRows + 2 * borderPadding;

        // Calculate the top-left coordinate to center the game board
        int totalBlockWidth = interval * blockAreaCols;
        int totalBlockHeight = interval * blockAreaRows;
        int startX = (getWidth() - totalBlockWidth) / 2;
        int startY = (getHeight() - totalBlockHeight) / 2;

        // Background panel
        g.setColor(Color.BLUE.darker().darker());
        g.fillRoundRect(startX, startY, totalBlockWidth, totalBlockHeight, interval, interval);

        // Clear the inner playable area
        g.setColor(Color.BLACK);
        g.fillRect(startX + interval * borderPadding, 
                    startY + interval * borderPadding, 
                    interval * gridCols, 
                    interval * gridRows);

        if (isPaused){return;}//prevents the player from planning in pause-time

        // Draw placed blocks
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                Color a = grid[row][col];
                if (a != null) {
                    g.setColor(a);
                    int x = startX + interval * (borderPadding + col);
                    int y = startY + interval * (borderPadding + row);
                    g.fillRect(x, y, interval, interval);
                }
            }
        }
       
        // Draw active blocks
        if (framesElapsed > 1){
            for (int row = 3; row < activeGrid.length; row++) {
                for (int col = 0; col < activeGrid[row].length; col++) {
                    Color a = activeGrid[row][col];
                    if (a != null) {
                        if (blockType.equals("line")){a = a.brighter();}
                        if (stall){
                            for (int[] i : blockLocation) {
                                if (i[0] == row && i[1] == col){
                                    a = a.darker();
                                }
                            }
                        }
                        g.setColor(a);
                        int x = startX + interval * (borderPadding + col);
                        int y = startY + interval * (borderPadding + row-3);
                        g.fillRect(x, y, interval, interval);
                    }
                }
            }
        }

         // Grid lines
        g.setColor(Color.WHITE.darker());
        for (int i = 0; i <= gridCols; i++) {
            int x = startX + interval * (borderPadding + i);
            g.drawLine(x, startY + interval * borderPadding, x, startY + interval * (borderPadding + gridRows));
        }
        for (int i = 0; i <= gridRows; i++) {
            int y = startY + interval * (borderPadding + i);
            g.drawLine(startX + interval * borderPadding, y, startX + interval * (borderPadding + gridCols), y);
        }
    }
    };

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
    }

    @Override public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
        handledKeys.remove(e.getKeyCode());
    }
    @Override public void keyTyped(KeyEvent e) {}

    public Tetris(){
        super();

        gameOver = false;
        isPaused = true;
        cycle = System.currentTimeMillis();

        setSize(FRAME_W, FRAME_H);
        setBackground(Color.BLACK);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });

        //keylistener
        addKeyListener(this);

        // Set layout for the main frame
        setLayout(new BorderLayout());
            
        //setting up the window
        setLocation((SCREEN_W - FRAME_W)/2, (SCREEN_H - FRAME_H)/2);
        
        //Adding game component
        gamePanel.setBackground(Color.BLACK);
        add(gamePanel, BorderLayout.CENTER);

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE); // For visibility on dark background
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setBackground(Color.BLACK);
        scoreLabel.setOpaque(true);
        add(scoreLabel, BorderLayout.NORTH);
    }

    private static void initMenu(Tetris a){
        //top menu bar
        JMenuBar menuBar = new JMenuBar();
        //menuBar.setBackground(Color.RED);

        JMenu graphicSettings = new JMenu("Graphics");

        //sets default size and location
        JMenuItem reset = new JMenuItem("Reset Window");
        reset.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    device.setFullScreenWindow(null);
                    a.setSize(FRAME_W, FRAME_H);
                    a.setLocation((SCREEN_W - FRAME_W)/2, (SCREEN_H - FRAME_H)/2);
                }
            });

        //toggles window fullscreen
        JMenuItem fullscreen = new JMenuItem("Toggle Fullscreen");
        fullscreen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (OS.indexOf("Mac") == -1){
                        if (a.getSize().getWidth() < SCREEN_W){
                            device.setFullScreenWindow(a);
                        }else{
                            device.setFullScreenWindow(null);
                        }
                    }else{
                        if (a.getSize().getWidth() < SCREEN_W){
                            a.setSize(SCREEN_W, SCREEN_H);
                            a.setLocation(0,0);
                        }else{
                            a.setSize(FRAME_W, FRAME_H);
                            a.setLocation((SCREEN_W - FRAME_W)/2, (SCREEN_H - FRAME_H)/2);
                        }
                    }
                }
            });

        //adding to graphics
        graphicSettings.add(reset);
        graphicSettings.add(fullscreen);

        //closes window
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    a.dispose();
                }
            });

        //play/pause game
        JMenuItem pausePlay = new JMenuItem("Play");
        pausePlay.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isPaused){
                        isPaused = false;
                        pausePlay.setText("Pause");
                    }else{
                        isPaused = true;
                        pausePlay.setText("Play");
                    }
                    gamePanel.repaint();
                }
        });

        //adding menu items
        menuBar.add(graphicSettings);
        menuBar.add(exit);
        menuBar.add(pausePlay);

        a.setJMenuBar(menuBar);
    }

    private static void endGame(){
        //endSequence
        System.out.println("Game Over");
    }

    private static void moveActiveDown(){
        stall = false; //resets cooldown
        for (int[] i : blockLocation){
            if (i[0] >= 22 || isOnFloor()){
                return;
            }
        }
        // Remove current block from grid
        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = null;
        }

        // Move block down
        for (int j = 0; j < blockLocation.length; j++){
            blockLocation[j][0]++;
        }

        // Place block in new position
        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = blockColor;
        }
    }

    private static boolean isOnFloor() {
        for (int j = blockLocation.length-1; j >=0; j--){
            int [] i = blockLocation[j];
            // out of the box or occupied by placed block
            if (i[0] - 2 >= grid.length || grid[i[0] -2][i[1]] != null) {
                //lose condition
                if (!blockType.equals("line") && i[0] <= 4 && findFullRow() == -1){
                    gameOver = true;
                }else if (i[0] <= 3 && findFullRow() == -1){
                    gameOver = true;
                }
                return true;
            }
        }
        return false;
    }

    private static void newBlock(String block){//this function sets active block into the grid, completes any full lines, and creates the new block
        //setting the active block into grid
        if (blockLocation != null){//not needed initially when no block has ever existed
            for (int[] i : blockLocation){
                grid[i[0]-3][i[1]] = blockColor;
                activeGrid[i[0]][i[1]] = null;
            }
        }

        completeRows();

        switch (block) {
            case "line":
                activeGrid[3][3] = Color.CYAN;
                activeGrid[3][4] = Color.CYAN;
                activeGrid[3][5] = Color.CYAN;
                activeGrid[3][6] = Color.CYAN;

                int[][] blockCoordsLine = {
                    {3, 5}, //block center
                    {3, 3},
                    {3, 4},
                    {3, 6}
                };
                blockLocation = blockCoordsLine;
                blockColor = Color.CYAN;
                blockType = "line";
                break;

            case "miniT":
                activeGrid[3][4] = Color.MAGENTA;
                activeGrid[4][3] = Color.MAGENTA;
                activeGrid[4][4] = Color.MAGENTA;
                activeGrid[4][5] = Color.MAGENTA;

                int[][] blockCoordsMiniT = {
                    {4, 4}, //block center
                    {3, 4},
                    {4, 3},
                    {4, 5}
                };
                blockLocation = blockCoordsMiniT;
                blockColor = Color.MAGENTA;
                blockType = "miniT";
                break;

            case "leftL":
                activeGrid[3][3] = Color.BLUE;
                activeGrid[4][3] = Color.BLUE;
                activeGrid[4][4] = Color.BLUE;
                activeGrid[4][5] = Color.BLUE;

                int[][] blockCoordsLeftL = {
                    {4, 4}, //block center
                    {3, 3},
                    {4, 3}, 
                    {4, 5}
                };
                blockLocation = blockCoordsLeftL;
                blockColor = Color.BLUE;
                blockType = "leftL";
                break;

            case "rightL":
                activeGrid[3][5] = Color.ORANGE;
                activeGrid[4][3] = Color.ORANGE;
                activeGrid[4][4] = Color.ORANGE;
                activeGrid[4][5] = Color.ORANGE;

                int[][] blockCoordsRightL = {
                    {4, 4},//block center
                    {3, 5},
                    {4, 3}, 
                    {4, 5}
                };
                blockLocation = blockCoordsRightL;
                blockColor = Color.ORANGE;
                blockType = "rightL";
                break;

            case "leftZ":
                activeGrid[3][5] = Color.GREEN;
                activeGrid[3][4] = Color.GREEN;
                activeGrid[4][4] = Color.GREEN;
                activeGrid[4][3] = Color.GREEN;

                int[][] blockCoordsLeftZ = {
                    {3, 4}, //block center
                    {3, 5},
                    {4, 4},
                    {4, 3}
                };
                blockLocation = blockCoordsLeftZ;
                blockColor = Color.GREEN;
                blockType = "leftZ";
                break;

            case "rightZ":
                activeGrid[3][3] = Color.RED;
                activeGrid[3][4] = Color.RED;
                activeGrid[4][4] = Color.RED;
                activeGrid[4][5] = Color.RED;

                int[][] blockCoordsRightZ = {
                    {3, 4}, //block center
                    {3, 3},
                    {4, 4},
                    {4, 5}
                };
                blockLocation = blockCoordsRightZ;
                blockColor = Color.RED;
                blockType = "rightZ";
                break;

            case "cube":
                activeGrid[3][5] = Color.YELLOW;
                activeGrid[3][4] = Color.YELLOW;
                activeGrid[4][4] = Color.YELLOW;
                activeGrid[4][5] = Color.YELLOW;

                int[][] blockCoordsCube = {
                    {3, 5},
                    {3, 4}, 
                    {4, 4},
                    {4, 5}
                };
                blockLocation = blockCoordsCube;
                blockColor = Color.YELLOW;
                blockType = "cube";
                break;

            default:
                throw new IllegalArgumentException("Unknown block type: " + block);
        }

    }

    private static void completeRows(){
        int fullRow = findFullRow();
        while (fullRow >= 0) {
            linesCompleted++;
            for (int j = fullRow; j > 0; j--) {
                grid[j] = grid[j - 1];
            }
            grid[0] = new Color[grid[0].length]; // clear top row
            fullRow = findFullRow();
        }
        SwingUtilities.invokeLater(() -> scoreLabel.setText("Score: " + linesCompleted));
    }

    private static int findFullRow() {
        for (int i = grid.length-1; i >= 0; i--){
            boolean isFull = true;
            for (Color a : grid[i]) {
                if (a == null){
                    isFull = false;
                    break;
                }
            }
            if (isFull){
                return i;
            }
        }
        return -1;
    }

    private static void moveRight(){//look at moveLeft for equivalent code with comments
        stallFrame += 2;
        for (int[] i : blockLocation) {
            if (i[0] >= 3){
                if (i[1] + 1 >= activeGrid[0].length || grid[i[0]-3][i[1]+1] != null){
                    System.out.println("StopGoingRight");
                    return;
                }
            }
        }

        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = null;
        }

        for (int j = 0; j < blockLocation.length; j++){
            blockLocation[j][1]++;
        }

        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = blockColor;
        }

        //gamePanel.repaint();
    }

    private static void moveLeft(){
        stallFrame += 2;
        // Check if movement to the left is possible
        for (int[] i : blockLocation) {
            if (i[0] >= 3){
                if (i[1] - 1 < 0 || grid[i[0] - 3][i[1] - 1] != null) {
                    System.out.println("StopGoingLeft");
                    return;
                }
            }
        }

        // Remove current block from grid
        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = null;
        }

        // Move block left
        for (int j = 0; j < blockLocation.length; j++){
            blockLocation[j][1]--;
        }

        // Place block in new position
        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = blockColor;
        }
    }

    private static void rotate(){//use inverse of points to rotate and reflections
        stall = false;//resets placement cooldown
        if (blockType.equals("cube")){return;}//cubes don't rotate

        // Remove current block from grid
        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = null;
        }

        //centering the block around the origin
        int rowOffset = blockLocation[0][0];
        int colOffset = blockLocation[0][1];

        int[][] centeredLocation = new int[4][2];

        for (int i = 0; i < centeredLocation.length; i++){
            centeredLocation[i][0] = blockLocation[i][0] - rowOffset;
            centeredLocation[i][1] = blockLocation[i][1] - colOffset;
        }

        //rotating the shape around the center
        for (int i = 0; i < centeredLocation.length; i++){
            int temp = centeredLocation[i][0];
            centeredLocation[i][0] = centeredLocation[i][1];
            centeredLocation[i][1] = -1 * temp;
        }

        //adds back in block offset
        for (int i = 0; i < centeredLocation.length; i++){
            blockLocation[i][0] = centeredLocation[i][0] + rowOffset;
            blockLocation[i][1] = centeredLocation[i][1] + colOffset;
        }

        int blockXDelta = 0;
        int blockYDelta = 0;
        //checks if rotated block is in grid and corrects it
        for (int j = 0; j < blockLocation.length; j++) {
            int[] i = blockLocation[j];
            if (i[1] > 9){
                blockXDelta--; 
            }
            if (i[1] < 0){
                blockXDelta++;
            }
            if (i[0] > 22){
                blockYDelta--;
            }
        }

        for (int k = 0; k < blockLocation.length; k++){
            blockLocation[k][1] += blockXDelta;
            blockLocation[k][0] += blockYDelta;
        }

        //checks if rotated shape overlaps
        boolean isInPlacedBlock = false;
        for (int j = blockLocation.length-1; j >=0; j--){
            int [] i = blockLocation[j];
            //occupied by placed block
            if (i[0] > 3 && grid[i[0]-3][i[1]] != null) {
                isInPlacedBlock = true;
            }
        }
        //shifts block up if it is in an existing block
        while (isInPlacedBlock){
            for (int k = 0; k < blockLocation.length; k++){
                blockLocation[k][0]--;
            }

            isInPlacedBlock = false;
            //checks if it overlaps
            for (int j = blockLocation.length-1; j >=0; j--){
                int [] i = blockLocation[j];
                //occupied by placed block
                if (i[0] == 2){
                    if (isOnFloor()){
                        return;
                    }else{
                        break;
                    }
                }
                if (grid[i[0] -3][i[1]] != null) {
                    isInPlacedBlock = true;
                }
            }
        }

        // Place block in new position
        for (int[] i : blockLocation){
            activeGrid[i[0]][i[1]] = blockColor;
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            Tetris frame = new Tetris();
            initMenu(frame);
            frame.setVisible(true);
            
            // Run game loop in a separate thread
            new Thread(() -> run()).start();
        });
    }

    private static void run(){
        //gameplay loop
        newBlock(queue[(int)(Math.random()*7)]); //Starting block
        while (!gameOver) {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= cycle + (1000 / frameRate)) {
                cycle = currentTime; // Update cycle each iteration
                framesElapsed++;
                if (!isPaused) {
                    checkInputs();
                    if (isOnFloor()){
                        if (!stall){
                            stall = true;
                            stallFrame = framesElapsed;
                        }
                        if(stallFrame + 30 <= framesElapsed){
                            stall = false;
                            newBlock(queue[(int)(Math.random()*7)]);
                        }
                    }
                    int blockInterval;
                    if (linesCompleted/5 <= 4){blockInterval = 15 - 2 * (linesCompleted / 5);}
                    else if(linesCompleted/5 <= 9){blockInterval = 15 - 8 - ((linesCompleted / 5) - 3);}
                    else{blockInterval = 1;}
                    if (framesElapsed % (blockInterval) == 0 && !isOnFloor()){
                        moveActiveDown();
                    }
                    gamePanel.repaint();
                }
            }
            try {
                Thread.sleep(1); // Yield to EDT
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        endGame();
    }

    private static void checkInputs(){
        if (pressedKeys.contains(KeyEvent.VK_UP) && !handledKeys.contains(KeyEvent.VK_UP)) {
            handledKeys.add(KeyEvent.VK_UP);
            rotate();
        }
        if (pressedKeys.contains(KeyEvent.VK_DOWN)) {
            if (framesElapsed % 5 == 0){
                if (!isOnFloor()) {
                    moveActiveDown();
                }
            }
        }
        if (pressedKeys.contains(KeyEvent.VK_LEFT)) {
            if (framesElapsed % 5 == 0){
                moveLeft();
            }
        }
        if (pressedKeys.contains(KeyEvent.VK_RIGHT)) {
            if (framesElapsed % 5 == 0){
                moveRight();
            }
        }
        if (pressedKeys.contains(KeyEvent.VK_SPACE)){
            while (!isOnFloor()){
                moveActiveDown();
            }
        }
    }
}
