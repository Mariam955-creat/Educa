package com.educa.backend.common.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

/**
 * Socle commun des PDF (certificats, factures) : langue, libellés traduits ({@code messages_*.properties}),
 * formats de date, et rendu HTML → PDF avec une police couvrant les six langues (DejaVu Sans, arabe compris)
 * et le support bidirectionnel/la mise en forme des glyphes arabes (ICU4J).
 */
@Component
public class PdfDocuments {

    /** Langues d'interface. Chiffres latins aussi en arabe, comme dans le frontend (usage Maghreb / Afrique de l'Ouest). */
    private static final Map<String, Locale> LOCALES = Map.of(
            "fr", Locale.FRANCE,
            "en", Locale.UK,
            "ar", Locale.forLanguageTag("ar-u-nu-latn"),
            "es", Locale.forLanguageTag("es-ES"),
            "pt", Locale.forLanguageTag("pt-PT"),
            "de", Locale.GERMANY);
    private static final String DEFAULT_LANG = "fr";
    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    public static final String FONT_FAMILY = "DejaVu Sans";

    private final MessageSource messages;
    private final byte[] regularFont = readFont("/fonts/DejaVuSans.ttf");
    private final byte[] boldFont = readFont("/fonts/DejaVuSans-Bold.ttf");

    public PdfDocuments(MessageSource messages) {
        this.messages = messages;
    }

    /** Langue demandée si elle est supportée, sinon la langue de repli (préférence du titulaire), sinon le français. */
    public String resolveLang(String requested, String fallback) {
        if (requested != null && LOCALES.containsKey(requested)) {
            return requested;
        }
        return fallback != null && LOCALES.containsKey(fallback) ? fallback : DEFAULT_LANG;
    }

    public Locale locale(String lang) {
        return LOCALES.getOrDefault(lang, Locale.FRANCE);
    }

    public boolean isRtl(String lang) {
        return "ar".equals(lang);
    }

    public String text(String key, String lang, Object... args) {
        return messages.getMessage(key, args.length == 0 ? null : args, locale(lang));
    }

    public String date(Instant instant, String lang) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale(lang)).withZone(ZONE).format(instant);
    }

    public String dateTime(Instant instant, String lang) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
                .withLocale(locale(lang)).withZone(ZONE).format(instant);
    }

    public byte[] render(String html, String lang) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> new ByteArrayInputStream(regularFont), FONT_FAMILY, 400, FontStyle.NORMAL, true);
            builder.useFont(() -> new ByteArrayInputStream(boldFont), FONT_FAMILY, 700, FontStyle.NORMAL, true);
            builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
            builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
            builder.defaultTextDirection(isRtl(lang) ? TextDirection.RTL : TextDirection.LTR);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Rendu PDF échoué", e);
        }
    }

    public static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static byte[] readFont(String path) {
        try (InputStream in = PdfDocuments.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Police absente du classpath : " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
