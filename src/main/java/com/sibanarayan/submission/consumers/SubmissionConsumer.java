package com.sibanarayan.submission.consumers;

import com.sibanarayan.submission.enums.ProgrammingLanguage;
import com.sibanarayan.submission.events.SubmissionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionConsumer {

    @KafkaListener(
            topics = "submission.events",
            groupId = "submission-service"
    )
    public void consume(SubmissionEvent event) {
        try{
            File codeFile=createFile(event.getSolution(),event.getLanguage());
            switch (event.getLanguage()){
                case PYTHON ->handlePythonSubmission(codeFile,event.getSolution());
            }

        }catch (Exception e){

        }
    }



    private File createFile(String code, ProgrammingLanguage language) throws IOException {
        String extension="";
        switch (language){
            case PYTHON ->
                extension=".py";

            case JAVA ->
                extension=".java";

            case JAVASCRIPT ->
                extension=".js";

            default->
                extension=".txt";

        }

        File file = File.createTempFile("submission_", extension);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(code);
        }
        return file;
    }

    public static String handlePythonSubmission(File codeFile, String input) throws Exception {

        String containerName = "judge_" + System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "--rm",
                "--memory=100m",
                "--cpus=0.5",
                "--network=none",
                "-v", codeFile.getAbsolutePath() + ":/app/Main.py",
                "judge-python",
                "python", "/app/Main.py"
        );

        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(input);
            writer.flush();
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        boolean finished = process.waitFor(2, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("TIME_LIMIT_EXCEEDED");
        }

        return output.toString().trim();
    }
}
