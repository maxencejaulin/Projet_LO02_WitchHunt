package com.view.graphic;

import com.controller.RoundController;
import com.model.card.RumourCard;
import com.model.card.effect.Effect;
import com.model.game.CardState;
import com.model.game.IdentityCard;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

public class Panel extends JPanel {
    //Constantes
    private static final int SIZE_FACTOR = 3;

    //Images
    private final Image background;
    private final Image cardFront;
    private final Image cardBack;

    private Graphics2D g2D;

    int mouseXPos, mouseYPos;

    enum Gradient {
        NAME,
        WITCH,
        HUNT
    }

    Panel() {
        this.background = getToolkit().getImage("data/Tabletop.jpg");
        this.cardFront = getToolkit().getImage("data/CardFrontEmpty.png");
        this.cardBack = getToolkit().getImage("data/CardBack.png");

        this.g2D = (Graphics2D) this.getGraphics();
    }

    /**
     * Get correct gradient to draw
     *
     * @param y        position of the text
     * @param gradient the gradient to pick
     * @return created gradient
     */
    private GradientPaint getGradient(int y, Gradient gradient) {
        FontMetrics fontMetrics = getFontMetrics(g2D.getFont());
        return switch (gradient) {
            case WITCH -> new GradientPaint(
                    0, y - fontMetrics.getHeight(),
                    new Color(255, 159, 64),
                    0, y + fontMetrics.getHeight(),
                    Color.BLACK
            );
            case HUNT -> new GradientPaint(
                    0, y - fontMetrics.getHeight(),
                    new Color(51, 153, 51),
                    0, y + fontMetrics.getHeight(),
                    Color.BLACK
            );
            default -> new GradientPaint(
                    0, y - 2 * fontMetrics.getHeight(),
                    Color.BLACK,
                    0, y + fontMetrics.getHeight(),
                    new Color(99, 125, 157)
            );
        };
    }

    //Draw a centered string on X axis
    private int drawXCenteredString(String string, int x, int y, int width) {
        //We need to forcefully split lines. The method drawString() doesn't apply \n
        if (string.contains("\n")) {
            String[] array = string.split("\n");
            for (int i = 0; i < array.length; i++) {
                drawXCenteredString(array[i], x, y + i * g2D.getFontMetrics(g2D.getFont()).getHeight(), width);
            }
            return array.length - 1;
        }
        g2D.drawString(string, x + (width - getFontMetrics(g2D.getFont()).stringWidth(string)) / 2, y);
        return 0;
    }

    private void drawEffects(int x, int y, int cardWidth, List<Effect> effects) {
        //Find good font
        Font font = g2D.getFont();

        //We compare each effect to make it so that the longest one fit in width
        int longestString = 0;
        for (Effect effect : effects) {
            String[] array = effect.toString().split("\n");
            for (int i = 0; i < array.length; i++) {
                int lengthOfCurrentString = g2D.getFontMetrics(font).stringWidth(array[i]);
                if (lengthOfCurrentString > longestString) {
                    longestString = lengthOfCurrentString;
                }
            }
        }
        //We set the font so that each effect fit
        g2D.setFont(font.deriveFont((float) (font.getSize() * cardWidth) / longestString));

        //Draw effects
        int yDelta = 0;
        int stringHeight = getFontMetrics(g2D.getFont()).getHeight();
        for (int i = 1; i <= effects.size(); i++) {
            int yPosition = y + (i + yDelta) * stringHeight;
            yDelta += drawXCenteredString(effects.get(i - 1).toString(), x, yPosition, cardWidth);
            //Draw line to separate effects
            if (i < effects.size()) {
                yPosition = y + (i + yDelta) * stringHeight;
                g2D.drawRect(x + cardWidth / 12, yPosition + stringHeight / 3, cardWidth - 2 * cardWidth / 12, 0);
            }
        }
    }

    //Draw a complete card
    private void drawCard(RumourCard rumourCard, int x, int y) {
        int cardHeight = getHeight() / SIZE_FACTOR, cardWidth = (int) (cardHeight / 1.35);

        //The card itself
        g2D.drawImage(cardFront, x, y, cardWidth, cardHeight, this);

        //Card name
        g2D.setFont(g2D.getFont().deriveFont((float) (getWidth() / 100)).deriveFont(Font.BOLD));
        g2D.setPaint(getGradient(y + cardHeight / 5, Gradient.NAME));
        drawXCenteredString(rumourCard.getCardName().toString(), x, y + cardHeight / 5, cardWidth);

        //Witch effects
        int effectRectangleY = y + cardHeight / 3;
        g2D.setColor(Color.decode("#ffebcc"));
        g2D.fillRect(x, effectRectangleY, cardWidth, cardHeight / 4);

        g2D.setFont(g2D.getFont().deriveFont((float) (getWidth() / 110)).deriveFont(Font.BOLD));
        g2D.setPaint(getGradient(effectRectangleY, Gradient.WITCH));
        drawXCenteredString("Witch?", x, effectRectangleY, cardWidth);

        g2D.setColor(Color.BLACK);
        g2D.setFont(g2D.getFont().deriveFont((float) (getWidth() / 200)));
        drawEffects(x, effectRectangleY, cardWidth, rumourCard.witchEffects);

        //Hunt effects
        effectRectangleY = (int) (y + (cardHeight / 1.5));
        g2D.setColor(Color.decode("#d6ebd6"));
        g2D.fillRect(x, effectRectangleY, cardWidth, cardHeight / 4);

        g2D.setFont(g2D.getFont().deriveFont((float) (getWidth() / 110)).deriveFont(Font.BOLD));
        g2D.setPaint(getGradient(effectRectangleY, Gradient.HUNT));
        drawXCenteredString("Hunt!", x, effectRectangleY, cardWidth);

        g2D.setColor(Color.BLACK);
        g2D.setFont(g2D.getFont().deriveFont((float) (getWidth() / 200)));
        drawEffects(x, effectRectangleY, cardWidth, rumourCard.huntEffects);
    }

    private void drawCard(int x, int y) {
        int cardHeight = getHeight() / SIZE_FACTOR, cardWidth = (int) (cardHeight / 1.35);

        //The card itself
        g2D.drawImage(cardBack, x, y, cardWidth, cardHeight, this);
    }

    private void drawHand(List<CardState> hand) {
        int cardWidth = ((int) (getHeight() / SIZE_FACTOR / 1.35));

        for (int i = 0; i < hand.size(); i++) {
            //even number of card : place around the center, odd : center them
            int centerFactor = getWidth() / 2;
            if (hand.size() % 2 != 0) centerFactor -= cardWidth / 2;

            int relativePosition = i - hand.size() / 2;

            int margin = signOfAbsolutePositive(relativePosition) * 10;
            if (hand.size() % 2 != 0) margin *= (int) Math.signum(i);

            //Settle problem with even on right side
            int nbOfMargin = hand.size() % 2 == 0 && i > 0 ?
                    Math.abs(relativePosition + 1 == 0 ? 1 : relativePosition + 1) :
                    Math.abs(relativePosition == 0 ? 1 : relativePosition);

            //Settle 2x margin in middle
            if (hand.size() % 2 == 0 && (relativePosition == -1 || relativePosition == 0)) {
                margin /= 2;
            }

            drawCard(
                    hand.get(i).rumourCard,
                    centerFactor + relativePosition * cardWidth + margin * nbOfMargin,
                    getHeight() - (10 + getHeight() / SIZE_FACTOR)
            );
        }
    }

    private int signOfAbsolutePositive(int i) {
        return i == 0 ? 1 : (int) Math.signum(i);
    }

    private void drawHand(int size) {
        int cardWidth = ((int) (getHeight() / SIZE_FACTOR / 1.35));

        for (int i = 0; i < size; i++) {
            int centerFactor = getWidth() / 2;
            if (size % 2 != 0) centerFactor -= cardWidth / 2;

            int relativePosition = i - size / 2;

            //Pour nb pair 2 du milieu moitié de marge

            int margin = signOfAbsolutePositive(relativePosition) * 10;
            if (size % 2 != 0) margin *= (int) Math.signum(i);

            //Settle problem with even on right side
            int nbOfMargin = size % 2 == 0 && i > 0 ?
                    Math.abs(relativePosition + 1 == 0 ? 1 : relativePosition + 1) :
                    Math.abs(relativePosition == 0 ? 1 : relativePosition);

            //Settle 2x margin in middle
            if (size % 2 == 0 && (relativePosition == -1 || relativePosition == 0)) {
                margin /= 2;
            }

            drawCard(
                    centerFactor + relativePosition * cardWidth + margin * nbOfMargin,
                    getHeight() - (10 + getHeight() / SIZE_FACTOR)
            );
        }
    }

    /**
     * Draw contained objects
     *
     * @param graphics basis needed for basic rendering operations
     */
    public void paintComponent(Graphics graphics) { //TODO Place action buttons somewhere
        super.paintComponents(graphics);

        g2D = (Graphics2D) graphics;
        RoundController roundController = RoundController.getRoundController();

        //Draw background
        graphics.drawImage(background, 0, 0, getWidth(), getHeight(), this);

        //Draw content
        if (roundController != null && roundController.identityCards.size() > 0) {
            //Draw hand of the current player at the bottom
            drawHand(RoundController.getCurrentPlayer().hand);


            //Draw the hand of each player hidden
            AffineTransform oldRotation = g2D.getTransform();
            double currentAngle = 0;

            for (IdentityCard card : roundController.identityCards) {
                if (card.player != RoundController.getCurrentPlayer()) {
                    double angle = (double) 360 / roundController.identityCards.size();
                    currentAngle += angle;

                    //Rotation to make all players on a circle
                    g2D.rotate(Math.toRadians(angle), (float) getWidth() / 2, (float) getHeight() / 2);

                    //The players on the side are set back a little to have some more room
                    AffineTransform temp = g2D.getTransform();
                    if (currentAngle % 180 != 0) {
                        int XTranslation = getWidth() / 40;
                        if ((currentAngle > 0 && currentAngle < 90) || (currentAngle > 180 && currentAngle < 270)) {
                            XTranslation = -XTranslation;
                        }
                        g2D.translate(XTranslation, getHeight() / 3);
                    }
                    //We draw the player hand
                    drawHand(card.player.hand.size());

                    //We reset change to the transform matrix
                    g2D.setTransform(temp);
                }
            }
            g2D.setTransform(oldRotation);

            //Draw the discard pile
        }
    }
}
