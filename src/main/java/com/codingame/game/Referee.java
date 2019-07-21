package com.codingame.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
    @Inject
    private MultiplayerGameManager<Player> gameManager;
    @Inject
    private GraphicEntityModule graphicEntityModule;

    private int[] board;
    private List<List<Sprite>> seeds;
    private List<Sprite> seedPool;
    private int[] seedPositionX;
    private int[] seedPositionY;

    @Override
    public void init() {
        board = new int[12];
        for (int i = 0; i < 12; i++) {
            board[i] = 4;
        }

        // Generate possible seed positions:
        seedPositionX = new int[26];
        seedPositionY = new int[26];
        seedPositionX[0] = seedPositionY[1] = 0;
        int v = 0;
        for (int i = 1; i < 7; i++) {
            seedPositionX[i] = (int) Math.round(40 * Math.cos(v));
            seedPositionY[i] = (int) Math.round(40 * Math.sin(v));
            v += i % 2 == 0 ? 3 : -1;
        }
        v = 1;
        for (int i = 7; i < 26; i++) {
            seedPositionX[i] = (int) Math.round(75 * Math.cos(0.33 * v));
            seedPositionY[i] = (int) Math.round(75 * Math.sin(0.33 * v));
            v += i % 2 == 0 ? 9 : -3;
        }

        drawBackground();
        drawHud();
        drawBoard();

        gameManager.setFrameDuration(2000);

        gameManager.setMaxTurns(100);
    }

    private void addSeed(int i) {
        Sprite seed = seedPool.remove(0);
        seed.setX(computeHouseX(i) + seedPositionX[seeds.get(i).size() % 26])
                .setY(computeHouseY(i) + seedPositionY[seeds.get(i).size() % 26]).setVisible(true);
        seeds.get(i).add(seed);
    }

    private void removeAllSeeds(int i) {
        seeds.get(i).forEach(seedToRemove -> {
            seedToRemove.setVisible(false);
            seedPool.add(seedToRemove);
        });
        seeds.get(i).clear();
    }

    private void drawBoard() {
        graphicEntityModule.createSprite().setImage("board.png").setAnchor(0.5).setX(1920 / 2).setY(1080 / 2);

        seeds = new ArrayList<List<Sprite>>();
        seedPool = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 12; i++) {
            seeds.add(new ArrayList<Sprite>());
            for (int j = 0; j < board[i]; j++) {
                Sprite seed = graphicEntityModule.createSprite()
                        .setImage(String.format("seed%d.png", random.nextInt(5))).setAnchor(.5)
                        .setRotation((i + 1) * (j + 1)).setScale(0.6 + Math.pow(0.5, 1 + Math.random()))
                        .setX(computeHouseX(i) + seedPositionX[j % 26]).setY(computeHouseY(i) + seedPositionY[j % 26]);
                seeds.get(i).add(seed);
            }
        }
    }

    private int computeHouseY(int i) {
        return i < 6 ? 690 : 410;
    }

    private int computeHouseX(int i) {
        if (i < 6) {
            return 330 + i * 254;
        } else {
            return 330 + (11 - i) * 254;
        }
    }

    private void drawBackground() {
        graphicEntityModule.createSprite().setImage("Background.jpg").setAnchor(0);
    }

    private void drawHud() {
        for (Player player : gameManager.getPlayers()) {
            int y = player.getIndex() == 0 ? 1080 - 150 : 150 - 60;
            int x = player.getIndex() == 0 ? 100 : 1920 - 100;
            int scoreX = player.getIndex() == 0 ? x + 100 : x - 100;
            int scoreY = player.getIndex() == 0 ? y - 20 : y + 20;

            graphicEntityModule.createRectangle().setWidth(140).setHeight(140).setX(x - 70).setY(y - 70).setLineWidth(0)
                    .setFillColor(player.getColorToken());

            graphicEntityModule.createRectangle().setWidth(120).setHeight(120).setX(x - 60).setY(y - 60).setLineWidth(0)
                    .setFillColor(0xffffff);

            Text text = graphicEntityModule.createText(player.getNicknameToken()).setX(x).setY(y + 120).setZIndex(20)
                    .setFontSize(40).setFillColor(0xffffff).setAnchor(0.5);

            Sprite avatar = graphicEntityModule.createSprite().setX(x).setY(y).setZIndex(20)
                    .setImage(player.getAvatarToken()).setAnchor(0.5).setBaseHeight(116).setBaseWidth(116);

            player.scoreText = graphicEntityModule.createText("Score: 0").setFontSize(40).setFillColor(0xffffff)
                    .setX(scoreX).setY(scoreY).setAnchor(player.getIndex());

            player.hud = graphicEntityModule.createGroup(text, avatar, player.scoreText);
            player.hud.setAlpha(0.2);
        }
    }

    private List<Action> computeValidActions(Player player) {
        List<Action> validActions = new ArrayList<>();
        if (player.getIndex() == 0) {
            boolean opponentCanPlay = false;
            for (int i = 6; i < 12; i++) {
                if (board[i] > 0)
                    opponentCanPlay = true;
            }

            for (int i = 0; i < 6; i++) {
                if (board[i] > 0 && (opponentCanPlay || board[i] >= 6 - i)) {
                    validActions.add(new Action(player, i));
                }
            }
        } else {
            boolean opponentCanPlay = false;
            for (int i = 0; i < 6; i++) {
                if (board[i] > 0)
                    opponentCanPlay = true;
            }

            for (int i = 6; i < 12; i++) {
                if (board[i] > 0 && (opponentCanPlay || board[i] >= 12 - i)) {
                    validActions.add(new Action(player, i));
                }
            }
        }
        return validActions;
    }

    private void sendInputs(Player player) {
        String p0Houses = String.format("%d %d %d %d %d %d", board[0], board[1], board[2], board[3], board[4], board[5]);
        String p1Houses = String.format("%d %d %d %d %d %d", board[6], board[7], board[8], board[9], board[10], board[11]);

        if (player.getIndex() == 0) {
            player.sendInputLine(p0Houses + " " + p1Houses);
        } else {
            player.sendInputLine(p1Houses + " " + p0Houses);
        }
    }

    private void setWinner(Player player) {
        gameManager.addToGameSummary(GameManager.formatSuccessMessage(player.getNicknameToken() + " won!"));
        endGame();
    }

    @Override
    public void gameTurn(int turn) {
        Player player = gameManager.getPlayer(turn % gameManager.getPlayerCount());

        List<Action> validActions = computeValidActions(player);
        if (validActions.isEmpty()) {
            for (int i = 0; i < 12; i++) {
                player.increaseScore(board[i]);
                removeAllSeeds(i);
                endGame();
            }
            return;
        }

        player.hud.setAlpha(1);

        sendInputs(player);
        player.execute();

        // Read inputs
        try {
            final Action action = player.getAction();
            gameManager.addToGameSummary(
                    String.format("Player %s played (%d)", action.player.getNicknameToken(), action.num));


            if (!validActions.contains(action)) {
                throw new InvalidAction("Invalid action.");
            }

            // Picking
            int seed = board[action.num];
            board[action.num] = 0;

            // Sowing
            List<Integer> toAdd = new ArrayList<>();
            int skip = 0;
            for (int i = 1; i <= seed; i++) {
                if ((action.num + i + skip) % 12 == action.num) {
                    skip++;
                }
                board[(action.num + i + skip) % 12]++;
                toAdd.add((action.num + i + skip) % 12);
            }

            // Capturing
            List<Integer> toRemove = new ArrayList<>();
            int current = (action.num + seed + skip) % 12;

            int[] boardBackup = board.clone();

            while ((current < 6 && player.getIndex() == 1 || current >= 6 && player.getIndex() == 0) && (board[current] == 2 || board[current] == 3)) {
                board[current] = 0;
                toRemove.add(current);
                current = (current + 11) % 12;
            }

            boolean isGrandSlam = true;
            int start = player.getIndex() == 0 ? 6 : 0;
            for (int i = 0; i < 6; i++) {
                if (board[start + i] > 0) {
                    isGrandSlam = false;
                    break;
                }
            }
            if (isGrandSlam) {
                // revert last action
                toRemove.clear();
                board[action.num] = seed;
            } else {
                removeAllSeeds(action.num);
                graphicEntityModule.commitWorldState(0);
    
                toRemove.forEach(i -> {
                    int captured = boardBackup[i];
                    player.increaseScore(captured);
                    gameManager.addTooltip(player, String.format("Captured %d (at %d)", captured, i));
                });
            }

            // Animations


            double step = 1.0 / (10 + toAdd.size() + toRemove.size());
            double t = step * 2;
            for (int i : toAdd) {
                addSeed(i);
                graphicEntityModule.commitWorldState(t);
                t += step;
            }
            t += step * 2;
            for (int i : toRemove) {
                removeAllSeeds(i);
                graphicEntityModule.commitWorldState(t);

                t += step;
            }

            gameManager.setFrameDuration(400 * (10 + toAdd.size() + toRemove.size()));

            // Check if win
            if (player.getScore() > 24) {
                setWinner(player);
            }
        } catch (NumberFormatException e) {
            player.deactivate("Wrong output!");
            player.setScore(-1);
            endGame();
        } catch (TimeoutException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " timeout!"));
            player.deactivate(player.getNicknameToken() + " timeout!");
            player.setScore(-1);
            endGame();
        } catch (InvalidAction e) {
            player.deactivate(e.getMessage());
            player.setScore(-1);
            endGame();
        }

        player.hud.setAlpha(0.2);
    }

    private void endGame() {
        gameManager.endGame();

        Player p0 = gameManager.getPlayers().get(0);
        Player p1 = gameManager.getPlayers().get(1);
        if (p0.getScore() > p1.getScore()) {
            p1.hud.setAlpha(0.3);
        }
        if (p0.getScore() < p1.getScore()) {
            p0.hud.setAlpha(0.3);
        }
    }
}
