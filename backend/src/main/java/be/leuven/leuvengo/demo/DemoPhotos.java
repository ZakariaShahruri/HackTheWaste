package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.domain.CleanlinessScale;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Draws an illustrated street scene for demo reports, so the feed has
 * pictures without passing off stock photos as real reports. The dirtier the
 * score, the more litter on the cobbles. Uses shapes only (no fonts), so it
 * also works on a bare server JRE.
 */
final class DemoPhotos {

    private DemoPhotos() {}

    static final int WIDTH = 800;
    static final int HEIGHT = 600;

    private static final int WALL_HEIGHT = 170;
    private static final int KERB_HEIGHT = 14;
    private static final int COBBLE_W = 46;
    private static final int COBBLE_H = 30;

    private static final Color WALL = new Color(0xD9CBB3);
    private static final Color DOOR = new Color(0x6E4B32);
    private static final Color KERB = new Color(0x6B6A66);
    private static final Color GROUT = new Color(0x77766F);
    private static final Color COBBLE = new Color(0x9C9A92);
    private static final Color BIN = new Color(0x2F5D3A);
    private static final Color BIN_LID = new Color(0x24492D);
    private static final Color SHADOW = new Color(0, 0, 0, 60);
    private static final Color[] CAN_COLORS = {new Color(0xC0392B), new Color(0xBDC3C7), new Color(0x2E86C1)};
    private static final Color[] BOTTLE_COLORS = {new Color(0x1E8449), new Color(0x7E5109)};
    private static final Color PAPER = new Color(0xF4F1EA);
    private static final Color PAPER_EDGE = new Color(0xC9C4B8);
    private static final Color[] LEAF_COLORS = {new Color(0xD68910), new Color(0xB9770E), new Color(0xF0B27A)};

    /** About one item per 3.5 points below 100: none on a spotless street, ~28 on a filthy one. */
    static int litterFor(int score) {
        return (int) Math.round((CleanlinessScale.MAX_SCORE - CleanlinessScale.clamp(score)) / 3.5);
    }

    static BufferedImage paint(int score, Random rnd) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintStreet(g, rnd);
            int binX = 80 + rnd.nextInt(WIDTH - 240);
            paintBin(g, binX, score < 40, rnd);
            for (int i = 0; i < litterFor(score); i++) {
                // Most litter ends up around the bin; the rest blows across the street.
                int x = rnd.nextDouble() < 0.6 ? binX + 40 + (int) (rnd.nextGaussian() * 90) : rnd.nextInt(WIDTH);
                int y = WALL_HEIGHT + 60 + rnd.nextInt(HEIGHT - WALL_HEIGHT - 80);
                paintItem(g, x, y, rnd);
            }
            for (int i = 0; i < 3 + rnd.nextInt(4); i++) {
                paintLeaf(g, rnd.nextInt(WIDTH), WALL_HEIGHT + 30 + rnd.nextInt(HEIGHT - WALL_HEIGHT - 40), rnd);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void paintStreet(Graphics2D g, Random rnd) {
        g.setColor(WALL);
        g.fillRect(0, 0, WIDTH, WALL_HEIGHT);
        g.setColor(DOOR);
        g.fillRoundRect(60 + rnd.nextInt(WIDTH - 180), 30, 70, WALL_HEIGHT - 30, 10, 10);
        g.setColor(GROUT);
        g.fillRect(0, WALL_HEIGHT + KERB_HEIGHT, WIDTH, HEIGHT);
        for (int row = 0, y = WALL_HEIGHT + KERB_HEIGHT; y < HEIGHT; row++, y += COBBLE_H) {
            int offset = row % 2 == 0 ? 0 : -COBBLE_W / 2;
            for (int x = offset; x < WIDTH; x += COBBLE_W) {
                g.setColor(jitter(COBBLE, 12, rnd));
                g.fillRoundRect(x + 2, y + 2, COBBLE_W - 4, COBBLE_H - 4, 10, 10);
            }
        }
        g.setColor(KERB);
        g.fillRect(0, WALL_HEIGHT, WIDTH, KERB_HEIGHT);
    }

    private static void paintBin(Graphics2D g, int x, boolean overflowing, Random rnd) {
        int top = WALL_HEIGHT - 60;
        g.setColor(SHADOW);
        g.fill(new Ellipse2D.Double(x - 6, top + 160, 122, 22));
        g.setColor(BIN);
        g.fill(new RoundRectangle2D.Double(x, top + 14, 110, 156, 14, 14));
        g.setColor(BIN_LID);
        g.fill(new RoundRectangle2D.Double(x - 6, top, 122, 20, 10, 10));
        if (overflowing) {
            for (int i = 0; i < 6; i++) {
                paintItem(g, x + 10 + rnd.nextInt(90), top - 4 + rnd.nextInt(12), rnd);
            }
        }
    }

    private static void paintItem(Graphics2D g, int x, int y, Random rnd) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.setColor(SHADOW);
        g.fill(new Ellipse2D.Double(-22, 8, 44, 14));
        g.rotate(rnd.nextDouble() * Math.PI * 2);
        switch (rnd.nextInt(5)) {
            case 0 -> paintCan(g, CAN_COLORS[rnd.nextInt(CAN_COLORS.length)]);
            case 1 -> paintBottle(g, BOTTLE_COLORS[rnd.nextInt(BOTTLE_COLORS.length)]);
            case 2 -> paintCup(g);
            case 3 -> paintBag(g);
            default -> paintPaper(g, rnd);
        }
        g.setTransform(saved);
    }

    private static void paintCan(Graphics2D g, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(-11, -20, 22, 40, 6, 6));
        g.setColor(color.brighter());
        g.fill(new Ellipse2D.Double(-11, -22, 22, 8));
    }

    private static void paintBottle(Graphics2D g, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(-10, -22, 20, 44, 8, 8));
        g.fill(new RoundRectangle2D.Double(-4, -36, 8, 16, 3, 3));
        g.setColor(new Color(255, 255, 255, 80));
        g.fill(new RoundRectangle2D.Double(-6, -16, 4, 30, 2, 2));
    }

    private static void paintCup(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillPolygon(new int[] {-12, 12, 9, -9}, new int[] {-16, -16, 16, 16}, 4);
        g.setColor(new Color(0xA0522D));
        g.fillRect(-11, -4, 22, 7);
    }

    private static void paintBag(Graphics2D g) {
        g.setColor(new Color(0x222222));
        g.fill(new Ellipse2D.Double(-26, -20, 52, 40));
        g.setColor(new Color(255, 255, 255, 50));
        g.fill(new Ellipse2D.Double(-14, -14, 16, 10));
    }

    private static void paintPaper(Graphics2D g, Random rnd) {
        int corners = 5 + rnd.nextInt(3);
        Polygon sheet = new Polygon();
        for (int i = 0; i < corners; i++) {
            double angle = 2 * Math.PI * i / corners;
            double radius = 14 + rnd.nextInt(14);
            sheet.addPoint((int) (Math.cos(angle) * radius), (int) (Math.sin(angle) * radius));
        }
        g.setColor(PAPER);
        g.fill(sheet);
        g.setColor(PAPER_EDGE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(sheet);
    }

    private static void paintLeaf(Graphics2D g, int x, int y, Random rnd) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.rotate(rnd.nextDouble() * Math.PI);
        g.setColor(LEAF_COLORS[rnd.nextInt(LEAF_COLORS.length)]);
        g.fill(new Ellipse2D.Double(-9, -4, 18, 8));
        g.setTransform(saved);
    }

    private static Color jitter(Color base, int amount, Random rnd) {
        int delta = rnd.nextInt(2 * amount + 1) - amount;
        return new Color(clampChannel(base.getRed() + delta), clampChannel(base.getGreen() + delta),
                clampChannel(base.getBlue() + delta));
    }

    private static int clampChannel(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
