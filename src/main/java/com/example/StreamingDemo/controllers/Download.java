package com.example.StreamingDemo.controllers;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping ("/api")
public class Download {

    private final Logger logger = LoggerFactory.getLogger(Download.class);

    @Value("classpath:files/63MB.pdf")
    Resource resourceFile;

    @GetMapping (value = "/downloadwithstreaming", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadwithstreaming() {
        StreamingResponseBody stream = out -> {
            final File file = resourceFile.getFile();

            if(file.exists()){
                if(file.isDirectory()){
                    final ZipOutputStream zipOut = new ZipOutputStream(out);
                    writeZipToStream(file,zipOut);
                }else{
                    writeFileToStream(file,out);
                }
            }
        };
        return new ResponseEntity(stream, HttpStatus.OK);
    }

    @GetMapping (value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> download(final HttpServletResponse response) throws IOException {
        final File file = resourceFile.getFile();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(file.exists()) {
            if (file.isDirectory()) {
                ZipOutputStream zipOut = new ZipOutputStream(baos);
                writeZipToStream(file, zipOut);
            } else {
                writeFileToStream(file,baos);
            }
        }
        return new ResponseEntity(baos.toByteArray(), HttpStatus.OK);
    }

    @GetMapping (value = "/downloadSplitted/itext", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadSplittedItext(final HttpServletResponse response) throws IOException {
        InputStream resourceFileInputStream = resourceFile.getInputStream();

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(resourceFileInputStream));
        PdfSplitter pdfSplitter = new PdfSplitter(pdfDoc);
        PdfDocument pdfExtractedDoc = pdfSplitter.extractPageRange(new PageRange("12000-12002"));
        pdfDoc.close();
        resourceFileInputStream.close();



        ByteArrayOutputStream bos = (ByteArrayOutputStream) pdfExtractedDoc.getWriter().getOutputStream();
        bos.toByteArray();
        pdfExtractedDoc.close();

        return new ResponseEntity(bos.toByteArray(), HttpStatus.OK);
    }

    @GetMapping (value = "/downloadSplitted/pdfbox", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadSplittedPdfBox(final HttpServletResponse response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream resourceFileInputStream = resourceFile.getInputStream();

        PDDocument document = PDDocument.load(resourceFileInputStream, MemoryUsageSetting.setupMixed(50 * 1024 * 1024));

        PageExtractor pageExtractor = new PageExtractor(document);
        pageExtractor.setStartPage(12000);
        pageExtractor.setEndPage(12002);


        PDDocument extractedDocument = pageExtractor.extract();
        extractedDocument.save(baos);
        extractedDocument.close();
        document.close();
        resourceFileInputStream.close();
        byte[] bytes = baos.toByteArray();
        baos.close();
        return new ResponseEntity(baos.toByteArray(), HttpStatus.OK);
    }
    private void printMemoryUsage(){
        Long memoryUsageInMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024L * 1024L);
        System.out.println(memoryUsageInMB +"MB");
    }

    private void writeFileToStream(File file,OutputStream outputStream){
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            byte[] bytes=new byte[1024];
            int length;
            while ((length=inputStream.read(bytes)) >= 0) {
                outputStream.write(bytes, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeZipToStream(File directory, ZipOutputStream zipOutputStream){
        if(directory.exists() && directory.isDirectory()) {
            try {
                for (final File file : directory.listFiles()) {
                    final InputStream inputStream=new FileInputStream(file);
                    final ZipEntry zipEntry=new ZipEntry(file.getName());
                    zipOutputStream.putNextEntry(zipEntry);
                    byte[] bytes=new byte[1024];
                    int length;
                    while ((length=inputStream.read(bytes)) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }
                    inputStream.close();
                }
                zipOutputStream.close();
            } catch (final IOException e) {
                logger.error("Exception while reading and streaming data {} ", e);
            }
        }
    }
}