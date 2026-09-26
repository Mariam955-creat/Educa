package com.educa.backend.support;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Texte extrait d'un PDF, pour vérifier la langue d'un certificat ou d'une facture générés. */
public final class PdfText {

    private PdfText() {
    }

    public static String of(byte[] pdf) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
