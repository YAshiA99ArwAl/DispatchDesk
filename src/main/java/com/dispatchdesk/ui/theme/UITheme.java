package com.dispatchdesk.ui.theme;

import java.awt.*;

public class UITheme {
    // Exact requested color palette: F4EEFF DCD6F7 A6B1E1 424874
    public static final Color BG_CANVAS         = new Color(0xF4, 0xEE, 0xFF); // #F4EEFF - Soft Lavender Canvas
    public static final Color ACCENT_LAVENDER   = new Color(0xDC, 0xD6, 0xF7); // #DCD6F7 - Soft Lilac / Accent
    public static final Color ACCENT_PERIWINKLE = new Color(0xA6, 0xB1, 0xE1); // #A6B1E1 - Slate Periwinkle
    public static final Color PRIMARY_DARK      = new Color(0x42, 0x48, 0x74); // #424874 - Deep Slate Navy / Indigo

    // Card & Surface Colors
    public static final Color CARD_BG           = Color.WHITE;
    public static final Color CARD_BORDER       = new Color(0xDC, 0xD6, 0xF7); // #DCD6F7
    public static final Color CARD_HOVER_BORDER = new Color(0xA6, 0xB1, 0xE1); // #A6B1E1

    // Text Hierarchy Colors
    public static final Color TEXT_PRIMARY      = new Color(0x42, 0x48, 0x74); // #424874 - Deep high-contrast text
    public static final Color TEXT_SECONDARY    = new Color(0x6D, 0x72, 0x98); // Slate muted text
    public static final Color TEXT_MUTED        = new Color(0x9A, 0x9E, 0xBA); // Subtle placeholder text

    // Subtle Pipeline Colors (harmonized with F4EEFF, DCD6F7, A6B1E1, 424874)
    public static final Color PIPELINE_DELIVERED = new Color(0x42, 0x48, 0x74); // #424874 - Deep primary tone
    public static final Color PIPELINE_PROGRESS  = new Color(0x71, 0x79, 0xA8); // Muted periwinkle indigo
    public static final Color PIPELINE_ASSIGNED  = new Color(0xA6, 0xB1, 0xE1); // #A6B1E1 - Soft slate periwinkle
    public static final Color PIPELINE_PENDING   = new Color(0xDC, 0xD6, 0xF7); // #DCD6F7 - Soft lavender
    public static final Color PIPELINE_FAILED    = new Color(0xC7, 0xB5, 0xCF); // Muted dusty mauve tint

    // Semantic Status Colors (Soft pastel badges matching modern SaaS UI)
    public static final Color STATUS_DELIVERED_BG = new Color(228, 246, 233);
    public static final Color STATUS_DELIVERED_FG = new Color(30, 130, 60);

    public static final Color STATUS_PROGRESS_BG  = new Color(0xDC, 0xD6, 0xF7); // #DCD6F7
    public static final Color STATUS_PROGRESS_FG  = new Color(0x42, 0x48, 0x74); // #424874

    public static final Color STATUS_PENDING_BG   = new Color(254, 243, 222);
    public static final Color STATUS_PENDING_FG   = new Color(194, 98, 12);

    public static final Color STATUS_ASSIGNED_BG  = new Color(232, 240, 254);
    public static final Color STATUS_ASSIGNED_FG  = new Color(26, 86, 219);

    public static final Color STATUS_FAILED_BG    = new Color(254, 232, 232);
    public static final Color STATUS_FAILED_FG    = new Color(200, 30, 30);

    // Modern Fonts (Segoe UI on Windows, with fallback)
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SECTION    = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_CARD_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_CARD_VALUE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BUTTON     = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BADGE      = new Font("Segoe UI", Font.BOLD, 11);

    public static void enableAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
