package ca.bcit.comp2522.quizapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A JavaFX-based quiz application that loads questions from a text file,
 * presents 10 random questions to the user, tracks the score,
 * and displays a summary of missed questions at the end.
 *
 * @author COMP2522
 * @version 1.0
 */
public class QuizApp extends Application
{
    private static final int    TOTAL_QUESTIONS        = 10;
    private static final int    SCENE_WIDTH_PX         = 500;
    private static final int    SCENE_HEIGHT_PX        = 400;
    private static final int    QUESTION_ANSWER_PARTS  = 2;
    private static final String QUIZ_FILE_NAME         = "quiz.txt";
    private static final String STYLESHEET_FILE_NAME   = "styles.css";
    private static final String PIPE_DELIMITER         = "\\|";

    private final List<String[]> allQuestions;
    private final List<String[]> selectedQuestions;
    private final List<String[]> missedQuestions;

    private Label     questionLabel;
    private Label     scoreLabel;
    private TextField answerField;
    private Button    submitButton;
    private Button    startButton;

    private int currentQuestionIndex;
    private int score;

    /**
     * Constructs a new QuizApp and initializes empty question lists.
     */
    public QuizApp()
    {
        allQuestions      = new ArrayList<>();
        selectedQuestions = new ArrayList<>();
        missedQuestions   = new ArrayList<>();
    }

    /**
     * Entry point for the application. Launches the JavaFX runtime.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args)
    {
        launch(args);
    }

    /**
     * Sets up the primary stage with all UI components and event handlers.
     * Loads quiz questions from the text file and configures the scene.
     *
     * @param primaryStage the primary stage provided by the JavaFX runtime
     */
    @Override
    public void start(final Stage primaryStage)
    {
        loadQuestions();

        questionLabel = new Label("Welcome to the Quiz App!");
        scoreLabel    = new Label("Score: 0 / " + TOTAL_QUESTIONS);
        answerField   = new TextField();
        submitButton  = new Button("Submit");
        startButton   = new Button("Start Quiz");

        answerField.setPromptText("Type your answer here");
        answerField.setDisable(true);
        submitButton.setDisable(true);

        startButton.setOnAction(event -> startQuiz());
        submitButton.setOnAction(event -> submitAnswer());

        answerField.setOnKeyPressed(event ->
        {
            if(event.getCode() == KeyCode.ENTER)
            {
                submitAnswer();
            }
        });

        final VBox root;
        root = new VBox(questionLabel, answerField, submitButton, startButton, scoreLabel);

        final Scene scene;
        scene = new Scene(root, SCENE_WIDTH_PX, SCENE_HEIGHT_PX);

        final String stylesheet;
        stylesheet = getClass().getResource(STYLESHEET_FILE_NAME).toExternalForm();
        scene.getStylesheets().add(stylesheet);

        primaryStage.setScene(scene);
        primaryStage.setTitle("JavaFX Quiz App");
        primaryStage.show();
    }

    /**
     * Reads all question-answer pairs from the quiz text file.
     * Each line must contain a question and answer separated by a pipe character.
     * Lines that do not conform to this format are skipped.
     */
    private void loadQuestions()
    {
        final InputStream inputStream;
        inputStream = getClass().getResourceAsStream(QUIZ_FILE_NAME);

        if(inputStream == null)
        {
            throw new RuntimeException("Quiz file not found on classpath: " + QUIZ_FILE_NAME);
        }

        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
        {
            String line;
            line = reader.readLine();

            while(line != null)
            {
                final String trimmedLine;
                trimmedLine = line.trim();

                if(!trimmedLine.isEmpty())
                {
                    final String[] parts;
                    parts = trimmedLine.split(PIPE_DELIMITER);

                    if(parts.length == QUESTION_ANSWER_PARTS)
                    {
                        allQuestions.add(new String[]{parts[0].trim(), parts[1].trim()});
                    }
                }

                line = reader.readLine();
            }
        }
        catch(final IOException e)
        {
            throw new RuntimeException("Failed to read quiz file: " + e.getMessage());
        }
    }

    /**
     * Starts or restarts the quiz by selecting 10 random questions,
     * resetting the score and question index, and displaying the first question.
     */
    private void startQuiz()
    {
        selectedQuestions.clear();
        missedQuestions.clear();

        final List<String[]> shuffled;
        shuffled = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffled);

        for(int i = 0; i < TOTAL_QUESTIONS && i < shuffled.size(); i++)
        {
            selectedQuestions.add(shuffled.get(i));
        }

        currentQuestionIndex = 0;
        score                = 0;

        scoreLabel.setText("Score: 0 / " + TOTAL_QUESTIONS);
        answerField.setDisable(false);
        submitButton.setDisable(false);
        startButton.setDisable(true);
        answerField.clear();

        displayCurrentQuestion();
    }

    /**
     * Displays the current question text in the question label.
     * The question number is shown along with the question text.
     */
    private void displayCurrentQuestion()
    {
        final String[] currentQuestion;
        currentQuestion = selectedQuestions.get(currentQuestionIndex);

        final int displayNumber;
        displayNumber = currentQuestionIndex + 1;

        questionLabel.setText("Q" + displayNumber + ": " + currentQuestion[0]);
    }

    /**
     * Submits the user's answer for the current question.
     * Compares the answer (case-insensitive) to the correct answer,
     * updates the score, records missed questions, and advances
     * to the next question or ends the quiz if all questions are answered.
     */
    private void submitAnswer()
    {
        if(answerField.isDisable())
        {
            return;
        }

        final String userAnswer;
        userAnswer = answerField.getText().trim();

        final String[] currentQuestion;
        currentQuestion = selectedQuestions.get(currentQuestionIndex);

        final String correctAnswer;
        correctAnswer = currentQuestion[1];

        if(userAnswer.equalsIgnoreCase(correctAnswer))
        {
            score++;
        }
        else
        {
            missedQuestions.add(currentQuestion);
        }

        scoreLabel.setText("Score: " + score + " / " + TOTAL_QUESTIONS);
        answerField.clear();
        currentQuestionIndex++;

        if(currentQuestionIndex < TOTAL_QUESTIONS)
        {
            displayCurrentQuestion();
        }
        else
        {
            endQuiz();
        }
    }

    /**
     * Ends the quiz by disabling input controls, displaying the final score,
     * and showing a summary of all missed questions with their correct answers.
     * Re-enables the start button so the user can restart.
     */
    private void endQuiz()
    {
        answerField.setDisable(true);
        submitButton.setDisable(true);
        startButton.setDisable(false);

        final StringBuilder summary;
        summary = new StringBuilder();
        summary.append("Quiz Over! Final Score: ")
                .append(score)
                .append(" / ")
                .append(TOTAL_QUESTIONS)
                .append("\n");

        if(missedQuestions.isEmpty())
        {
            summary.append("Perfect score! No missed questions.");
        }
        else
        {
            summary.append("\nMissed Questions:\n");

            for(final String[] missed : missedQuestions)
            {
                summary.append("Q: ")
                        .append(missed[0])
                        .append(" -> A: ")
                        .append(missed[1])
                        .append("\n");
            }
        }

        questionLabel.setText(summary.toString());
    }
}