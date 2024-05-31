import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

public class Hangman extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;

    private JLabel wordLabel, statusLabel, usedLabel, statsLabel;
    private JTextField inputField;
    private JButton guessButton, resetButton, editButton;
    private List<String> words;
    private String chosenWord;
    private List<Character> correctLetters, incorrectLetters;
    private int attemptsLeft;
    private DifficultyLevel difficultyLevel;

    private int wins;
    private int losses;
    private int totalAttempts;

    private static final String PREF_WINS = "wins";
    private static final String PREF_LOSSES = "losses";
    private static final String PREF_TOTAL_ATTEMPTS = "totalAttempts";
    private Preferences preferences;

    enum DifficultyLevel {
        ŁATWY(7),
        ŚREDNI(5),
        TRUDNY(3);

        private final int maxAttempts;

        DifficultyLevel(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }
    }

    public Hangman() {
        setTitle("Wisielec");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        preferences = Preferences.userNodeForPackage(Hangman.class);

        wordLabel = new JLabel();
        statusLabel = new JLabel();
        usedLabel = new JLabel("Użyte litery: ");
        statsLabel = new JLabel();
        inputField = new JTextField(10);
        inputField.addActionListener(this);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Podaj literę lub całe słowo: "));
        topPanel.add(inputField);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        centerPanel.add(wordLabel);
        centerPanel.add(statusLabel);
        centerPanel.add(statsLabel);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.add(usedLabel);
        guessButton = new JButton("Zgaduj");
        guessButton.addActionListener(this);
        bottomPanel.add(guessButton);

        resetButton = new JButton("Reset");
        resetButton.addActionListener(this);
        bottomPanel.add(resetButton);

        editButton = new JButton("Edytuj słowa");
        editButton.addActionListener(this);
        bottomPanel.add(editButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        loadStatistics();
        initializeGame();
    }

    private void initializeGame() {
        correctLetters = new ArrayList<>();
        incorrectLetters = new ArrayList<>();
        attemptsLeft = difficultyLevel != null ? difficultyLevel.getMaxAttempts() : 0;
        usedLabel.setText("Użyte litery: ");
        loadWords();
        chooseWord();
        updateWordLabel();
        updateStatusLabel();
        inputField.setEnabled(true);
        guessButton.setEnabled(true);
        resetButton.setEnabled(true);
        askForDifficultyLevel();
        updateStatsLabel();
    }

    private void askForDifficultyLevel() {
        String[] options = {"Łatwy", "Średni", "Trudny"};
        int choice = JOptionPane.showOptionDialog(this,
                "Wybierz poziom trudności:",
                "Poziom trudności",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0:
                difficultyLevel = DifficultyLevel.ŁATWY;
                break;
            case 1:
                difficultyLevel = DifficultyLevel.ŚREDNI;
                break;
            case 2:
                difficultyLevel = DifficultyLevel.TRUDNY;
                break;
            default:
                difficultyLevel = DifficultyLevel.ŁATWY;
                break;
        }

        attemptsLeft = difficultyLevel.getMaxAttempts();
        updateStatusLabel();
    }

    private void loadWords() {
        words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Błąd ładowania słów: " + e.getMessage(),
                    "Błąd pliku",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveWords() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("words.txt"))) {
            for (String word : words) {
                writer.write(word);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Błąd zapisu słów: " + e.getMessage(),
                    "Błąd pliku",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseWord() {
        Random random = new Random();
        chosenWord = words.get(random.nextInt(words.size()));
    }

    private void updateWordLabel() {
        StringBuilder wordDisplay = new StringBuilder();
        for (int i = 0; i < chosenWord.length(); i++) {
            char c = chosenWord.charAt(i);
            if (correctLetters.contains(c)) {
                wordDisplay.append(c).append(" ");
            } else {
                wordDisplay.append("_ ");
            }
        }
        wordLabel.setText(wordDisplay.toString());
    }

    private void updateStatusLabel() {
        statusLabel.setText("Pozostałe próby: " + attemptsLeft);
    }

    private void updateStatsLabel() {
        statsLabel.setText("Wygrane: " + wins + " | Przegrane: " + losses + " | Całkowite próby: " + totalAttempts);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == guessButton || e.getSource() == inputField) {
            String input = inputField.getText().toLowerCase().trim();
            inputField.setText("");
            if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
                processLetter(input.charAt(0));
            } else if (input.length() > 1) {
                processWord(input);
            }
            updateStatusLabel();
        } else if (e.getSource() == resetButton) {
            resetStatistics();
            initializeGame();
        } else if (e.getSource() == editButton) {
            editWordList();
        }
    }

    private void processLetter(char guessedLetter) {
        if (!incorrectLetters.contains(guessedLetter)) {
            totalAttempts++;
            if (chosenWord.contains(String.valueOf(guessedLetter))) {
                correctLetters.add(guessedLetter);
            } else {
                incorrectLetters.add(guessedLetter);
                attemptsLeft--;
            }
            updateWordLabel();
            updateUsedLetters();
            checkGameStatus();
        }
    }

    private void processWord(String guessedWord) {
        totalAttempts++;
        if (guessedWord.equals(chosenWord)) {
            correctLetters.clear();
            for (int i = 0; i < chosenWord.length(); i++) {
                correctLetters.add(chosenWord.charAt(i));
            }
            updateWordLabel();
            checkGameStatus();
            resetButton.setEnabled(true);
        } else {
            incorrectLetters.clear();
            for (char c : guessedWord.toCharArray()) {
                if (!chosenWord.contains(String.valueOf(c))) {
                    incorrectLetters.add(c);
                }
            }
            attemptsLeft--;
            updateUsedLetters();
            updateStatusLabel();
            checkGameStatus();
        }
    }

    private void updateUsedLetters() {
        StringBuilder usedLettersDisplay = new StringBuilder("Użyte litery: ");
        for (char c : incorrectLetters) {
            usedLettersDisplay.append(c).append(" ");
        }
        usedLabel.setText(usedLettersDisplay.toString());
    }

    private void checkGameStatus() {
        if (correctLetters.size() == chosenWord.length()) {
            wins++;
            saveStatistics();
            endGame("Gratulacje! Zgadłeś słowo: " + chosenWord);
        } else if (attemptsLeft <= 0) {
            losses++;
            saveStatistics();
            endGame("Przegrałeś! Słowo to: " + chosenWord);
        }
        updateStatsLabel();
    }

    private void endGame(String message) {
        int choice = JOptionPane.showConfirmDialog(this,
                message + "\nCzy chcesz zagrać ponownie?",
                "Koniec Gry",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            initializeGame();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Dziękuję za grę w Wisielca!",
                    "Koniec Gry",
                    JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    private void editWordList() {
        JTextArea textArea = new JTextArea(20, 40);
        JScrollPane scrollPane = new JScrollPane(textArea);

        for (String word : words) {
            textArea.append(word + "\n");
        }

        int option = JOptionPane.showConfirmDialog(this,
                scrollPane,
                "Edytuj słowa",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String[] lines = textArea.getText().split("\\n");
            words.clear();
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    words.add(line.trim().toLowerCase());
                }
            }
            saveWords();
        }
    }

    private void loadStatistics() {
        wins = preferences.getInt(PREF_WINS, 0);
        losses = preferences.getInt(PREF_LOSSES, 0);
        totalAttempts = preferences.getInt(PREF_TOTAL_ATTEMPTS, 0);
    }

    private void saveStatistics() {
        preferences.putInt(PREF_WINS, wins);
        preferences.putInt(PREF_LOSSES, losses);
        preferences.putInt(PREF_TOTAL_ATTEMPTS, totalAttempts);
    }

    private void resetStatistics() {
        wins = 0;
        losses = 0;
        totalAttempts = 0;
        saveStatistics();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Hangman hangman = new Hangman();
            hangman.setVisible(true);
        });
    }
}

