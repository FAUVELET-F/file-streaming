package com.example.StreamingDemo.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping (value = "/downloadwithstreaming", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadwithstreaming(final HttpServletResponse response) {

        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment;filename=sample.zip");

        StreamingResponseBody stream = out -> {

            final String home = System.getProperty("user.home");
            final File directory = new File(home +File.separator + "OneDrive - Harmonie Mutuelle" + File.separator + "Test");
            final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
            logger.info(System.getProperty(directory.getAbsolutePath()));
            if(directory.exists() && directory.isDirectory()) {
                try {
                    for (final File file : directory.listFiles()) {
                        final InputStream inputStream=new FileInputStream(file);
                        final ZipEntry zipEntry=new ZipEntry(file.getName());
                        zipOut.putNextEntry(zipEntry);
                        byte[] bytes=new byte[1024];
                        int length;
                        while ((length=inputStream.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }
                        inputStream.close();
                    }
                    zipOut.close();
                } catch (final IOException e) {
                    logger.error("Exception while reading and streaming data {} ", e);
                }
            }
        };
        logger.info("steaming response {} ", stream);
        return new ResponseEntity(stream, HttpStatus.OK);
    }

    @GetMapping (value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> download(final HttpServletResponse response) {
        final String home = System.getProperty("user.home");
        final File directory = new File(home +File.separator + "OneDrive - Harmonie Mutuelle" + File.separator + "Test");
        ZipOutputStream zipOut = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            zipOut = new ZipOutputStream(baos);
            for (final File file : directory.listFiles()) {
                final InputStream inputStream=new FileInputStream(file);
                final ZipEntry zipEntry=new ZipEntry(file.getName());
                zipOut.putNextEntry(zipEntry);
                byte[] bytes=new byte[1024];
                int length;
                while ((length=inputStream.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                inputStream.close();
            }
            zipOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return new ResponseEntity(baos.toByteArray(), HttpStatus.OK);
    }
}