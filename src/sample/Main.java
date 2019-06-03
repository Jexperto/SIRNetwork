package sample;

import XMLparser.Document;
import XMLparser.Node;
import XMLparser.XMLParser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;
import java.util.regex.Pattern;

import static java.lang.Math.abs;


public class Main extends Application {
    private Cell[][] cells;
    private Document document;
    private int firstNumber;
    private int secondNumber;
    private boolean currentTorus;
    private double canvasWidth;
    private double canvasHeight;
    private String newUnhealthyValue;
    private String newHealthyValue;
    private String newSuscepribleValue;
    private double circleRadius = 8;
    private double offset = 40;
    private double heightScale;
    private double widthScale;
    private boolean threadState = true;
    private int contaminatedCells = 0;
    private int resistantCells = 0;
    private int susceptibleCells = 0;
    private TextField unhealthyPercentage;
    private TextField healthyPercentage;
    private TextField susceptiblePercentage;
    private Label resistantCount;
    private Label susceptibleCount;
    private Label contaminatedCount;
    private boolean isBubble = false;
    private Thread emulationThread;
    private XYChart.Series contaminatedSeries;
    private XYChart.Series susceptibleSeries;
    private XYChart.Series resistantSeries;
    private final static int STATE_CONTAMINATED = 100;
    private final static int STATE_SUSCEPTIBLE = 101;
    private final static int STATE_RESISTANT = 102;
    private final static int STATE_DELETED = 103;
    private final static Color BLOODY_RED = new Color(0.7725, 0, 0, 1);
    private final static Color SUSCEPTIBLE_YELLOW = new Color(0.851, 0.874, 0.011, 1);
    private final static Color HEALTHY_GREEN = new Color(0.298, 0.611, 0.239, 1);
    private final static Color TRANSPARENT = new Color(0, 0, 0, 0);


    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Симуляционная сеть");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        resistantCount = (Label) root.lookup("#resistantCount");
        susceptibleCount = (Label) root.lookup("#susceptibleCount");
        contaminatedCount = (Label) root.lookup("#contaminatedCount");
        TextField data = (TextField) root.lookup("#data");
        Button button = (Button) root.lookup("#button");
        Button plotButton = (Button) root.lookup("#plot");
        plotButton.setDisable(true);
        Button saveButton = (Button) root.lookup("#save");
        saveButton.setDisable(true);
        Button loadButton = (Button) root.lookup("#load");
        Button emulate = (Button) root.lookup("#emulate");
        CheckBox torus = (CheckBox) root.lookup("#torus");
        CheckBox limiter = (CheckBox) root.lookup("#limiter");


        Canvas canvas1 = (Canvas) root.lookup("#canvas1");
        canvasHeight = canvas1.getHeight();
        canvasWidth = canvas1.getWidth();
        GraphicsContext gc1 = canvas1.getGraphicsContext2D();
        gc1.setFill(Color.WHITE);
        gc1.fillRect(0, 0, canvasWidth, canvasHeight);


        Canvas canvas2 = (Canvas) root.lookup("#canvas2");
        canvasHeight = canvas2.getHeight();
        canvasWidth = canvas2.getWidth();
        GraphicsContext gc2 = canvas2.getGraphicsContext2D();
        gc2.setFill(Color.WHITE);
        gc2.fillRect(0, 0, canvasWidth, canvasHeight);

        unhealthyPercentage = (TextField) root.lookup("#unhealthyPercentage");
        healthyPercentage = (TextField) root.lookup("#healthyPercentage");
        susceptiblePercentage = (TextField) root.lookup("#susceptiblePercentage");


        unhealthyPercentage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!newValue && newUnhealthyValue.equals(""))
                    unhealthyPercentage.setText("0");
            } catch (Exception e) {
            }
        });
        healthyPercentage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!newValue && newHealthyValue.equals(""))
                    healthyPercentage.setText("0");
            } catch (Exception e) {
            }
        });
        susceptiblePercentage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!newValue && newSuscepribleValue.equals(""))
                    susceptiblePercentage.setText("0");
            } catch (Exception e) {
            }
        });


        unhealthyPercentage.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                newUnhealthyValue = newValue;
                double value = 100 - Double.parseDouble(healthyPercentage.getText()) - Double.parseDouble(susceptiblePercentage.getText());
                if (limiter.isSelected())
                    if (Double.parseDouble(newValue) > value)
                        unhealthyPercentage.setText(Double.toString(value));
            } catch (Exception e) {
            }
        });

        healthyPercentage.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                newHealthyValue = newValue;
                double value = 100 - Double.parseDouble(unhealthyPercentage.getText()) - Double.parseDouble(susceptiblePercentage.getText());
                if (limiter.isSelected())
                    if (Double.parseDouble(newValue) > value)
                        healthyPercentage.setText(Double.toString(value));
            } catch (Exception e) {
            }
        });
        susceptiblePercentage.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                newSuscepribleValue = newValue;
                double value = 100 - Double.parseDouble(healthyPercentage.getText()) - Double.parseDouble(unhealthyPercentage.getText());
                if (limiter.isSelected())
                    if (Double.parseDouble(newValue) > value)
                        susceptiblePercentage.setText(Double.toString(value));
            } catch (Exception e) {
            }
        });

        button.setOnAction(event -> {
            String d = data.getText();
            try {
                String[] parts = d.split(Pattern.quote("*"), 2);
                firstNumber = Integer.parseUnsignedInt(parts[0]);
                if (parts.length == 1)
                    secondNumber = firstNumber;
                else
                    secondNumber = Integer.parseUnsignedInt(parts[1]);
                contaminatedCells = 0;
                susceptibleCells = 0;
                resistantCells = 0;
//                plotButton.setDisable(false);
                createNetwork(torus.isSelected());

                // Проверка соседей
//                Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                alert.setTitle("Info");
//                int a, b;
//                a = 0;
//                b = 0;
//                alert.setContentText(a + " ; " + b + "\n" + String.valueOf(cells[a][b].neighbors.up) + " - верх\n" + String.valueOf(cells[a][b].neighbors.down) + " - низ\n" + String.valueOf(cells[a][b].neighbors.left) + " - лево\n" + String.valueOf(cells[a][b].neighbors.right) + " - право");
//                alert.showAndWait();


                canvas2.toFront();
                gc1.clearRect(0, 0, canvasWidth, canvasHeight);
                gc2.clearRect(0, 0, canvasWidth, canvasHeight);
                saveButton.setDisable(false);
                drawNetwork(gc1, gc2);

            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Wrong input");
                alert.showAndWait();
            }
        });

        plotButton.setOnAction(event -> {
            Stage stage = new Stage();
            stage.show();
            stage.setTitle("График состояния системы");
            final CategoryAxis xAxis = new CategoryAxis();
            final NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Модельное время");

            final LineChart<String, Number> lineChart =
                    new LineChart<String, Number>(xAxis, yAxis);

            lineChart.setTitle("Зависимость параметров системы от времени");

            contaminatedSeries.setName("Зараженные");

            susceptibleSeries.setName("Восприимчивые");
            resistantSeries.setName("Здоровые");
            Scene scene = new Scene(lineChart, 800, 600);
            lineChart.getData().addAll(contaminatedSeries, susceptibleSeries, resistantSeries);

            stage.setScene(scene);
            stage.show();

        });

        saveButton.setOnAction(event -> {
            document = Document.createDocument();
            Node sim = new Node("net_sim");
            document.appendChild(sim);
            sim.setAttribute("cont", "" + unhealthyPercentage.getText());
            sim.setAttribute("res", "" + healthyPercentage.getText());
            sim.setAttribute("susc", "" + susceptiblePercentage.getText());
            sim.setAttribute("torus", "" + torus.isSelected());
            Node sim2 = new Node("net");
            sim2.setAttribute("h", "" + firstNumber);
            sim2.setAttribute("w", "" + secondNumber);
            sim.appendChild(sim2);
            Node line;
            Node node;
            for (int i = 0; i < firstNumber; i++) {
                line = new Node("line");
                line.setAttribute("i", "" + i);
                for (int j = 0; j < secondNumber; j++) {
                    node = new Node("node");
                    node.setAttribute("j", "" + j);
                    node.setAttribute("state", "" + cells[i][j].getState());
                    Node near = new Node("near");
                    near.setAttribute("up", "" + cells[i][j].neighbors.up);
                    near.setAttribute("down", "" + cells[i][j].neighbors.down);
                    near.setAttribute("left", "" + cells[i][j].neighbors.left);
                    near.setAttribute("right", "" + cells[i][j].neighbors.right);
                    node.appendChild(near);

                    line.appendChild(node);
                }
                sim2.appendChild(line);
            }
            sim2 = new Node("graph");
            sim.appendChild(sim2);
            try {
                XMLParser.save("net.xml", document);
            } catch (IOException ignored) {
            }
        });

        loadButton.setOnAction(event -> {
            try {
                contaminatedCells = 0;
                susceptibleCells = 0;
                resistantCells = 0;
                document = XMLParser.parse("net.xml");
                Node sim = document.getNodesByTagName("net_sim").get(0);
                unhealthyPercentage.setText(sim.getAttribute("cont"));
                healthyPercentage.setText(sim.getAttribute("res"));
                susceptiblePercentage.setText(sim.getAttribute("susc"));
                torus.setSelected(Boolean.parseBoolean(sim.getAttribute("torus")));
                Node sim2 = sim.getNodesByTagName("net").get(0);
                firstNumber = Integer.parseInt(sim2.getAttribute("h"));
                secondNumber = Integer.parseInt(sim2.getAttribute("w"));
                cells = new Cell[firstNumber][secondNumber];
                if (firstNumber != secondNumber)
                    data.setText("" + firstNumber + "*" + secondNumber);
                else
                    data.setText("" + firstNumber);
                for (Node line : sim2.getNodesByTagName("line")) {
                    int i = Integer.parseInt(line.getAttribute("i"));
                    for (Node node : line.getNodesByTagName("node")) {
                        int j = Integer.parseInt(node.getAttribute("j"));
                        int state = Integer.parseInt(node.getAttribute("state"));
                        cells[i][j] = new Cell(i, j, state);
                        switch (state) {
                            case STATE_CONTAMINATED:
                                contaminatedCells++;
                                break;
                            case STATE_RESISTANT:
                                resistantCells++;
                                break;
                            case STATE_SUSCEPTIBLE:
                                susceptibleCells++;
                                break;
                        }
                    }
                }
                for (Node line : sim2.getNodesByTagName("line")) {
                    int i = Integer.parseInt(line.getAttribute("i"));
                    for (Node node : line.getNodesByTagName("node")) {
                        int j = Integer.parseInt(node.getAttribute("j"));
                        for (Node near : node.getNodesByTagName("near")) {
                            int up = Integer.parseInt(near.getAttribute("up"));
                            int down = Integer.parseInt(near.getAttribute("down"));
                            int left = Integer.parseInt(near.getAttribute("left"));
                            int right = Integer.parseInt(near.getAttribute("right"));
                            cells[i][j].setNeighbors(new Cell.Connections(up, down, left, right));
                        }
                    }
                }

                canvas2.toFront();
                gc1.clearRect(0, 0, canvasWidth, canvasHeight);
                gc2.clearRect(0, 0, canvasWidth, canvasHeight);
                drawNetwork(gc1, gc2);
                saveButton.setDisable(false);
            } catch (IOException e) {
            }
        });


        EventHandler<javafx.scene.input.MouseEvent> clickHandler =
                e -> {
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.WHITE);
                    WritableImage canvas = canvas2.snapshot(params, null);
                    PixelReader reader = canvas.getPixelReader();
                    Color dotColor = reader.getColor((int) e.getX(), (int) e.getY());

                    if (((int) (dotColor.getRed() * 255) <= ((int) (BLOODY_RED.getRed() * 255) + 20) && (int) (dotColor.getRed() * 255) >= ((int) (BLOODY_RED.getRed() * 255) - 20) &&
                            (int) (dotColor.getGreen() * 255) <= ((int) (BLOODY_RED.getGreen() * 255) + 20) && (int) (dotColor.getGreen() * 255) >= ((int) (BLOODY_RED.getGreen() * 255) - 20) &&
                            (int) (dotColor.getBlue() * 255) <= ((int) (BLOODY_RED.getBlue() * 255) + 20) && (int) (dotColor.getBlue() * 255) >= ((int) (BLOODY_RED.getBlue() * 255) - 20)) ||

                            ((int) (dotColor.getRed() * 255) <= ((int) (SUSCEPTIBLE_YELLOW.getRed() * 255) + 20) && (int) (dotColor.getRed() * 255) >= ((int) (SUSCEPTIBLE_YELLOW.getRed() * 255) - 20) &&
                                    (int) (dotColor.getGreen() * 255) <= ((int) (SUSCEPTIBLE_YELLOW.getGreen() * 255) + 20) && (int) (dotColor.getGreen() * 255) >= ((int) (SUSCEPTIBLE_YELLOW.getGreen() * 255) - 20) &&
                                    (int) (dotColor.getBlue() * 255) <= ((int) (SUSCEPTIBLE_YELLOW.getBlue() * 255) + 20) && (int) (dotColor.getBlue() * 255) >= ((int) (SUSCEPTIBLE_YELLOW.getBlue() * 255) - 20)) ||

                            ((int) (dotColor.getRed() * 255) <= ((int) (HEALTHY_GREEN.getRed() * 255) + 20) && (int) (dotColor.getRed() * 255) >= ((int) (HEALTHY_GREEN.getRed() * 255) - 20) &&
                                    (int) (dotColor.getGreen() * 255) <= ((int) (HEALTHY_GREEN.getGreen() * 255) + 20) && (int) (dotColor.getGreen() * 255) >= ((int) (HEALTHY_GREEN.getGreen() * 255) - 20) &&
                                    (int) (dotColor.getBlue() * 255) <= ((int) (HEALTHY_GREEN.getBlue() * 255) + 20) && (int) (dotColor.getBlue() * 255) >= ((int) (HEALTHY_GREEN.getBlue() * 255) - 20))) {

                        tackleCircle(e.getX(), e.getY(), gc2, e.getButton());
                    } else {
                        Coords xy1;
                        Coords xy2;
                        for (int i = 0; i < firstNumber; i++) {         //clear lines
                            heightScale = (canvasHeight - offset * 2) / (firstNumber - 1);
                            widthScale = (canvasWidth - offset * 2) / (secondNumber - 1);
                            if (e.getY() >= i * heightScale + offset - 5 && e.getY() <= i * heightScale + offset + 5) {
                                for (int j = 0; j < secondNumber; j++) {
                                    if (j * widthScale + offset >= e.getX()) {
//                                                if (cells[i][j].neighbors.left > 0) {
//                                                    cells[i][j].neighbors.left = -1;
//                                                    cells[i][j - 1].neighbors.right = -1;
//                                                    xy1 = new Coords(j * widthScale, i * heightScale + circleRadius);
//                                                    xy2 = new Coords((j - 1) * widthScale, i * heightScale + circleRadius);
//                                                    gc1.clearRect(xy2.x, xy2.y, abs(xy2.x - xy1.x), 2);
//                                                } else {
//                                                    cells[i][j].neighbors.left = j - 1;
//                                                    cells[i][j - 1].neighbors.right = j;
//                                                    xy1 = new Coords(j * widthScale, i * heightScale + circleRadius);
//                                                    xy2 = new Coords((j - 1) * widthScale, i * heightScale + circleRadius);
//                                                    gc1.setFill(Color.rgb(0x7a, 0x7a, 0x7a));
//                                                    gc1.fillRect(xy2.x, xy2.y, abs(xy2.x - xy1.x), 2);
//                                                }
                                        createLine(i, j, gc1, true);
                                        break;
                                    }
                                }
                                break;
                            }

                        }
                        for (int j = 0; j < secondNumber; j++) {            //clear columns
                            heightScale = (canvasHeight - offset * 2) / (firstNumber - 1);
                            widthScale = (canvasWidth - offset * 2) / (secondNumber - 1);
                            if (e.getX() >= j * widthScale + offset - 5 && e.getX() <= j * widthScale + offset + 5) {
                                for (int i = 0; i < firstNumber; i++) {
                                    if (i * heightScale + offset >= e.getY()) {
//                                                    if (cells[i][j].neighbors.up > 0) {
//                                                        cells[i][j].neighbors.up = -1;
//                                                        cells[i - 1][j].neighbors.down = -1;
//                                                        xy1 = new Coords(j * widthScale + circleRadius, i * heightScale);
//                                                        xy2 = new Coords(j * widthScale + circleRadius, (i - 1) * heightScale);
//                                                        gc1.clearRect(xy2.x, xy2.y, 2, abs(xy2.y - xy1.y));
//                                                    } else {
//                                                        cells[i][j].neighbors.up = i - 1;
//                                                        cells[i - 1][j].neighbors.down = i;
//                                                        xy1 = new Coords(j * widthScale + circleRadius, i * heightScale);
//                                                        xy2 = new Coords(j * widthScale + circleRadius, (i - 1) * heightScale);
//                                                        gc1.setFill(Color.rgb(0x7a, 0x7a, 0x7a));
//                                                        gc1.fillRect(xy2.x, xy2.y, 2, abs(xy2.y - xy1.y));
//                                                    }
                                        createLine(i, j, gc1, false);
                                        break;
                                    }

                                }
                                break;
                            }
                        }
                    }
                };

        canvas2.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, clickHandler);
        emulate.setOnAction(event -> {
            plotButton.setDisable(false);
            if (threadState) {
                threadState = false;
                emulate.setText("Стоп");
                Runnable r = () -> {
                    try {
                        int timer = 0;
                        while (contaminatedCells > 0 || contaminatedCells < firstNumber * secondNumber) {
                            double heightScale = (canvasHeight - offset * 2) / (firstNumber - 1);
                            double widthScale = (canvasWidth - offset * 2) / (secondNumber - 1);
                            boolean changed = false;
                            boolean changedContaminated = false;

                            contaminatedSeries.getData().add(new XYChart.Data(Integer.toString(timer), contaminatedCells));
                            susceptibleSeries.getData().add(new XYChart.Data(Integer.toString(timer), susceptibleCells));
                            resistantSeries.getData().add(new XYChart.Data(Integer.toString(timer), resistantCells));
                            timer++;
                            for (int i = 0; i < firstNumber; i++) {
                                for (int j = 0; j < secondNumber; j++) {
                                    int currentCellState = cells[i][j].getState();
                                    Random rng = new Random();
                                    if (currentCellState == STATE_CONTAMINATED) {
                                        gc2.setFill(BLOODY_RED);
                                        double probability = Double.parseDouble(unhealthyPercentage.getText()) / 100;
                                        if (cells[i][j].neighbors.up > -1) {
                                            if (currentTorus && i == 0) {
                                                if (cells[firstNumber - 1][j].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[firstNumber - 1][j].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + (firstNumber - 1) + " " + j + " " + contaminatedCells);
                                                        }
                                                        cells[firstNumber - 1][j].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect(j * widthScale + offset - circleRadius, (firstNumber - 1) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval(j * widthScale + offset - circleRadius, (firstNumber - 1) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            } else {
                                                if (cells[i - 1][j].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[i - 1][j].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + (i - 1) + " " + j + " " + contaminatedCells);
                                                        }
                                                        cells[i - 1][j].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect(j * widthScale + offset - circleRadius, (i - 1) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval(j * widthScale + offset - circleRadius, (i - 1) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            }
                                        }
                                        if (cells[i][j].neighbors.down > -1) {
                                            if (currentTorus && i == firstNumber - 1) {
                                                if (cells[0][j].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[0][j].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + (0) + " " + j + " " + contaminatedCells);

                                                        }
                                                        cells[0][j].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect(j * widthScale + offset - circleRadius, (0) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval(j * widthScale + offset - circleRadius, (0) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;

                                                    }
                                                }
                                            } else {
                                                if (cells[i + 1][j].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[i + 1][j].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + (i + 1) + " " + j + " " + contaminatedCells);

                                                        }
                                                        cells[i + 1][j].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect(j * widthScale + offset - circleRadius, (i + 1) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval(j * widthScale + offset - circleRadius, (i + 1) * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            }
                                        }
                                        if (cells[i][j].neighbors.left > -1) {
                                            if (currentTorus && j == 0) {
                                                if (cells[i][secondNumber - 1].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[i][secondNumber - 1].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + i + " " + (secondNumber - 1) + " " + contaminatedCells);
                                                        }
                                                        cells[i][secondNumber - 1].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect((secondNumber - 1) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval((secondNumber - 1) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            } else {
                                                if (cells[i][j - 1].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[i][j - 1].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + i + " " + (j - 1) + " " + contaminatedCells);
                                                        }
                                                        cells[i][j - 1].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect((j - 1) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval((j - 1) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            }
                                        }
                                        if (cells[i][j].neighbors.right > -1) {
                                            if (currentTorus && j == secondNumber - 1) {
                                                if (cells[i][0].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[i][0].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + i + " " + (0) + " " + contaminatedCells);
                                                        }
                                                        cells[i][0].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect((0) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval((0) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            } else {
                                                if (cells[i][j + 1].getState() == STATE_SUSCEPTIBLE) {
                                                    if (rng.nextDouble() <= probability) {
                                                        if (cells[i][j + 1].getNewState() == STATE_SUSCEPTIBLE) {
                                                            contaminatedCells++;
                                                            susceptibleCells--;
                                                            System.out.println("Contaminated: " + i + " " + (j + 1) + " " + contaminatedCells);
                                                        }
                                                        cells[i][j + 1].setNewState(STATE_CONTAMINATED);
                                                        gc2.clearRect((j + 1) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        gc2.fillOval((j + 1) * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                                        changedContaminated = true;
                                                    }
                                                }
                                            }
                                        }

                                        if (rng.nextDouble() <= Double.parseDouble(healthyPercentage.getText()) / 100) {
                                            changed = true;
                                            cells[i][j].setNewState(STATE_RESISTANT);
                                            contaminatedCells--;
                                            resistantCells++;
                                            gc2.setFill(HEALTHY_GREEN);
                                            gc2.clearRect(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                            gc2.fillOval(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                            System.out.println();
                                            System.out.println("Resistant: " + i + " " + j + " " + resistantCells);
                                        }
                                    } else if (currentCellState == STATE_RESISTANT) {
                                        rng = new Random();
                                        if (rng.nextDouble() <= Double.parseDouble(susceptiblePercentage.getText()) / 100) {
                                            changed = true;
                                            cells[i][j].setNewState(STATE_SUSCEPTIBLE);
                                            resistantCells--;
                                            susceptibleCells++;
                                            gc2.setFill(SUSCEPTIBLE_YELLOW);
                                            gc2.clearRect(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                            gc2.fillOval(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);


                                        }

                                    }
                                    if (changed) {
                                        //   gc2.setFill(cells[i][j].getColor());
                                        //    gc2.fillOval(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
                                    }
                                }

                            }
                            // System.out.println("kek");
                            if (changedContaminated || changed)
                                for (int i = 0; i < firstNumber; i++) {
                                    for (int j = 0; j < secondNumber; j++) {
                                        cells[i][j].setState(cells[i][j].getNewState());
                                    }
                                }


                            Platform.runLater(() -> {
                                resistantCount.setText(Integer.toString(resistantCells));
                                contaminatedCount.setText(Integer.toString(contaminatedCells));
                                susceptibleCount.setText(Integer.toString(susceptibleCells));
                            });
                            Thread.sleep(300);
                        }
                    } catch (Exception e) {
                        System.out.println("Thread has been interrupted");
                        System.out.println(e.getMessage());
                    }

                };
                emulationThread = new Thread(r, "emulationThread");
                emulationThread.start();

            } else {
                try {
                    emulationThread.interrupt();
                    emulate.setText("Эмулировать");
                    threadState = true;
                } catch (Exception e) {
                }
            }


        });
    }

    private void createLine(int i, int j, GraphicsContext gc1, boolean horizontal) {
        Coords xy1;
        Coords xy2;
        boolean border = false;
        if (!horizontal) {
            if (cells[i][j].neighbors.up >= 0) {
                cells[i][j].neighbors.up = -1;
                cells[i - 1][j].neighbors.down = -1;
                xy1 = new Coords(j * widthScale + offset, i * heightScale + offset);
                xy2 = new Coords(j * widthScale + offset, (i - 1) * heightScale + offset);
                gc1.clearRect(xy2.x, xy2.y, 2, abs(xy2.y - xy1.y));
            } else {
                cells[i][j].neighbors.up = i - 1;
                cells[i - 1][j].neighbors.down = i;
                xy1 = new Coords(j * widthScale + offset, i * heightScale + offset);
                xy2 = new Coords(j * widthScale + offset, (i - 1) * heightScale + offset);
                gc1.setFill(Color.rgb(0x7a, 0x7a, 0x7a));
                gc1.fillRect(xy2.x, xy2.y, 1, abs(xy2.y - xy1.y));
            }

        } else {
            if (cells[i][j].neighbors.left >= 0) {
                cells[i][j].neighbors.left = -1;
                cells[i][j - 1].neighbors.right = -1;
                xy1 = new Coords(j * widthScale + offset, i * heightScale + offset);
                xy2 = new Coords((j - 1) * widthScale + offset, i * heightScale + offset);
                gc1.clearRect(xy2.x, xy2.y, abs(xy2.x - xy1.x), 2);
            } else {
                cells[i][j].neighbors.left = j - 1;
                cells[i][j - 1].neighbors.right = j;
                xy1 = new Coords(j * widthScale + offset, i * heightScale + offset);
                xy2 = new Coords((j - 1) * widthScale + offset, i * heightScale + offset);
                gc1.setFill(Color.rgb(0x7a, 0x7a, 0x7a));
                gc1.fillRect(xy2.x, xy2.y, abs(xy2.x - xy1.x), 1);
            }
        }
    }

    private void drawNetwork(GraphicsContext gc1, GraphicsContext gc2) {
        double heightScale = (canvasHeight - offset * 2) / (firstNumber - 1);
        double widthScale = (canvasWidth - offset * 2) / (secondNumber - 1);
        gc2.setFill(SUSCEPTIBLE_YELLOW);
        gc1.setFill(Color.rgb(0x7a, 0x7a, 0x7a));
        gc1.setLineWidth(1);
//
//        for (int i = 0; i < firstNumber; i++) {
//            gc1.strokeLine(circleRadius + offset, i * heightScale + circleRadius + offset, widthScale * (secondNumber - 1) + circleRadius + offset, i * heightScale + circleRadius + offset);
//        }
//        for (int i = 0; i < secondNumber; i++) {
//            gc1.strokeLine(i * widthScale + circleRadius + offset, circleRadius + offset, i * widthScale + circleRadius + offset, heightScale * (firstNumber - 1) + circleRadius + offset);
//        }
        for (int i = 0; i < firstNumber; i++) {
            for (int j = 0; j < secondNumber; j++) {
                double x1 = j * widthScale + offset;
                double y1 = i * heightScale + offset;
                double x2 = (j + 1) * widthScale + offset;
                double y2 = (i + 1) * heightScale + offset;
                if (j != secondNumber - 1 && i != firstNumber - 1) {
                    if (cells[i][j].neighbors.right != -1)
                        gc1.fillRect(x1, y1, x2 - x1, 1);
                    if (cells[i][j].neighbors.down != -1)
                        gc1.fillRect(x1, y1, 1, y2 - y1);
                } else if (j == secondNumber - 1 && i != firstNumber - 1) {
                    if (cells[i][j].neighbors.down != -1)
                        gc1.fillRect(x1, y1, 1, y2 - y1);
                } else if (j != secondNumber - 1 && i == firstNumber - 1)
                    if (cells[i][j].neighbors.right != -1)
                        gc1.fillRect(x1, y1, x2 - x1, 1);

            }
        }
        for (int i = 0; i < firstNumber; i++) {
            for (int j = 0; j < secondNumber; j++) {

                if (cells[i][j].getState() == STATE_CONTAMINATED)
                    gc2.setFill(BLOODY_RED);
                else if (cells[i][j].getState() == STATE_RESISTANT)
                    gc2.setFill(HEALTHY_GREEN);
                else if (cells[i][j].getState() == STATE_SUSCEPTIBLE)
                    gc2.setFill(SUSCEPTIBLE_YELLOW);
                else if (cells[i][j].getState() == STATE_DELETED)
                    gc2.setFill(TRANSPARENT);

                double x = j * widthScale + offset - circleRadius;
                double y = i * heightScale + offset - circleRadius;
                gc2.clearRect(x, y, circleRadius * 2, circleRadius * 2);
                gc2.fillOval(x, y, circleRadius * 2, circleRadius * 2);

                // System.out.println("x = " + x + " y = " + y + " radius = " + circleRadius);
            }

        }
    }

    private void tackleCircle(double pixelX, double pixelY, GraphicsContext gc2, MouseButton btn) {
        double heightScale = (canvasHeight - offset * 2) / (firstNumber - 1);
        double widthScale = (canvasWidth - offset * 2) / (secondNumber - 1);
//        int i = BinaryCellSearch(cells, pixelX / widthScale, 0, secondNumber - 1, false);
//        int j = BinaryCellSearch(cells, pixelY / heightScale, 0, secondNumber - 1, true);
        int j = (int) Math.round((pixelX - offset) / widthScale);
        int i = (int) Math.round((pixelY - offset) / heightScale);

        if (btn == MouseButton.PRIMARY) {
            if (cells[i][j].getState() == STATE_RESISTANT) {
                cells[i][j].setState(STATE_CONTAMINATED);
                cells[i][j].setNewState(STATE_CONTAMINATED);

                contaminatedCells++;
                resistantCells--;
            } else if (cells[i][j].getState() == STATE_CONTAMINATED) {
                cells[i][j].setState(STATE_SUSCEPTIBLE);
                cells[i][j].setNewState(STATE_SUSCEPTIBLE);

                contaminatedCells--;
                susceptibleCells++;
            } else if (cells[i][j].getState() == STATE_SUSCEPTIBLE) {
                cells[i][j].setState(STATE_RESISTANT);
                cells[i][j].setNewState(STATE_RESISTANT);
                resistantCells++;
                susceptibleCells--;
            }
        }
        if (btn == MouseButton.SECONDARY) {
            if (cells[i][j].getState() == STATE_RESISTANT) {
                cells[i][j].setState(STATE_DELETED);
                cells[i][j].setNewState(STATE_DELETED);
                resistantCells--;
            } else if (cells[i][j].getState() == STATE_CONTAMINATED) {
                cells[i][j].setState(STATE_DELETED);
                cells[i][j].setNewState(STATE_DELETED);
                contaminatedCells--;
            } else if (cells[i][j].getState() == STATE_SUSCEPTIBLE) {
                cells[i][j].setState(STATE_DELETED);
                cells[i][j].setNewState(STATE_DELETED);
                susceptibleCells--;
            }
        }
        resistantCount.setText(Integer.toString(resistantCells));
        contaminatedCount.setText(Integer.toString(contaminatedCells));
        susceptibleCount.setText(Integer.toString(susceptibleCells));
        Color color = cells[i][j].getColor();
        if (!color.equals(TRANSPARENT)) {
            gc2.setFill(cells[i][j].getColor());
            gc2.clearRect(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
            gc2.fillOval(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
        } else {
            gc2.clearRect(j * widthScale + offset - circleRadius, i * heightScale + offset - circleRadius, circleRadius * 2, circleRadius * 2);
        }
    }

    private void tackleLine() {


    }

//    public int BinaryLineSearch(Cell[][] sortedArray, double key, int low, int high) {
//        int index = Integer.MAX_VALUE;
//        if (sourceKey) {
//            while (low <= high) {
//                int mid = (low + high) / 2;
//                if (sortedArray[1][mid].j < key) {
//                    low = mid + 1;
//                } else if (sortedArray[1][mid].j > key) {
//                    high = mid - 1;
//                } else if (sortedArray[1][mid].j == key) {
//                    low = mid;
//                    break;
//                }
//            }
//            return sortedArray[1][low].j;
//        } else {
//            while (low <= high) {
//                int mid = (low + high) / 2;
//                if (sortedArray[mid][1].i < key) {
//                    low = mid + 1;
//                } else if (sortedArray[mid][1].i > key) {
//                    high = mid - 1;
//                } else if (sortedArray[mid][1].i == key) {
//                    low = mid;
//                    break;
//                }
//            }
//        }
//        return sortedArray[low][1].i;
//    }

    private void createNetwork(boolean TOR) {
        currentTorus = TOR;
        cells = new Cell[firstNumber][secondNumber];
        for (int i = 0; i < firstNumber; i++) {
            for (int j = 0; j < secondNumber; j++) {
                int nodeState;
                double rng = Math.random();
                if (rng <= Double.parseDouble(unhealthyPercentage.getText()) / 100) {
                    nodeState = STATE_CONTAMINATED;
                    contaminatedCells++;
                } else if (rng <= Double.parseDouble(unhealthyPercentage.getText()) / 100 + Double.parseDouble(healthyPercentage.getText()) / 100) {
                    nodeState = STATE_RESISTANT;
                    resistantCells++;
                } else {
                    nodeState = STATE_SUSCEPTIBLE;
                    susceptibleCells++;
                }
                cells[i][j] = new Cell(i, j, nodeState);
                int up, down, left, right;
                if (TOR) {
                    up = (i > 0) ? i - 1 : firstNumber - 1;
                    down = (i < firstNumber - 1) ? i + 1 : 0;
                    left = (j > 0) ? j - 1 : secondNumber - 1;
                    right = (j < secondNumber - 1) ? j + 1 : 0;
                } else {
                    up = (i > 0) ? i - 1 : -1;
                    down = (i < firstNumber - 1) ? i + 1 : -1;
                    left = (j > 0) ? j - 1 : -1;
                    right = (j < secondNumber - 1) ? j + 1 : -1;
                }

                cells[i][j].neighbors = new Cell.Connections(up, down, left, right);
            }

        }
        resistantCount.setText(Integer.toString(resistantCells));
        contaminatedCount.setText(Integer.toString(contaminatedCells));
        susceptibleCount.setText(Integer.toString(firstNumber * secondNumber - resistantCells - contaminatedCells));
        resistantSeries = new XYChart.Series();
        susceptibleSeries = new XYChart.Series();
        contaminatedSeries = new XYChart.Series();
    }


    public static void main(String[] args) {
        launch(args);
    }


    private static class Cell {
        private int i;                      //координата x в матрице
        private int j;                     //координата y в матрице
        private Color color;              //цвет узла
        private int state;               //состояние узла
        private int newState;               //новое состояние узла
        private Connections neighbors;  //соседи узла

        Cell(int i, int j, int state) {
            this.i = i;
            this.j = j;
            this.state = state;
            this.newState = state;

            switch (state) {
                case STATE_CONTAMINATED:
                    this.color = BLOODY_RED;
                    break;
                case STATE_RESISTANT:
                    this.color = HEALTHY_GREEN;
                    break;
                case STATE_SUSCEPTIBLE:
                    this.color = SUSCEPTIBLE_YELLOW;
                    break;
                case STATE_DELETED:
                    this.color = TRANSPARENT;
                    break;
                default:
                    this.color = SUSCEPTIBLE_YELLOW;
                    break;
            }
        }

        private static class Connections {       // контейнер для соседей
            private int up;
            private int down;
            private int left;
            private int right;

            Connections(int up, int down, int left, int right) {
                this.up = up;
                this.down = down;
                this.left = left;
                this.right = right;
            }

            public int getUp() {
                return up;
            }

            public int getDown() {
                return down;
            }

            public int getLeft() {
                return left;
            }

            public int getRight() {
                return right;
            }

            public void setUp(int up) {
                this.up = up;
            }

            public void setDown(int down) {
                this.down = down;
            }

            public void setLeft(int left) {
                this.left = left;
            }

            public void setRight(int right) {
                this.right = right;
            }
        }


        public void setI(int i) {
            this.i = i;
        }

        public void setJ(int j) {
            this.j = j;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void setNewState(int newState) {
            this.newState = newState;

            switch (state) {
                case STATE_CONTAMINATED:
                    this.color = BLOODY_RED;
                    break;
                case STATE_RESISTANT:
                    this.color = HEALTHY_GREEN;
                    break;
                case STATE_SUSCEPTIBLE:
                    this.color = SUSCEPTIBLE_YELLOW;
                    break;
                case STATE_DELETED:
                    this.color = TRANSPARENT;
                    break;
                default:
                    this.color = SUSCEPTIBLE_YELLOW;
                    break;
            }

        }

        public void setState(int state) {
            this.state = state;

            switch (state) {
                case STATE_CONTAMINATED:
                    this.color = BLOODY_RED;
                    break;
                case STATE_RESISTANT:
                    this.color = HEALTHY_GREEN;
                    break;
                case STATE_SUSCEPTIBLE:
                    this.color = SUSCEPTIBLE_YELLOW;
                    break;
                case STATE_DELETED:
                    this.color = TRANSPARENT;
                    break;
                default:
                    this.color = SUSCEPTIBLE_YELLOW;
                    break;
            }

        }


        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        public Color getColor() {
            return color;
        }

        public int getState() {
            return state;
        }

        public int getNewState() {
            return newState;
        }

        public Connections getNeighbors() {
            return neighbors;
        }

        public void setNeighbors(Connections neighbors) {
            this.neighbors = neighbors;
        }
    }

    private static class Coords {
        private double x;
        private double y;

        public Coords(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void setX(double x) {
            this.x = x;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    private static class EmulationThread implements Runnable {


        public void run() {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread has been interrupted");
            }
        }
    }

}


