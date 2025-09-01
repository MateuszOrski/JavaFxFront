package com.example.javafxfront;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class StudentService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String STUDENTS_ENDPOINT = BASE_URL + "/students";

    private final HttpClient httpClient;

    public StudentService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Dodaje studenta na serwer
     */
    public CompletableFuture<Student> addStudentAsync(Student student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = studentToJson(student);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    return student; // Zwracamy oryginalnego studenta
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie dodac studenta na serwer: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Usuwa studenta z serwera
     */
    public CompletableFuture<Boolean> deleteStudentAsync(String studentIndexNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT + "/" + studentIndexNumber))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200 || response.statusCode() == 204;

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie usunac studenta z serwera: " + e.getMessage(), e);
            }
        });
    }

    private String studentToJson(Student student) {
        return String.format(
                "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"indexNumber\":\"%s\",\"groupName\":\"%s\"}",
                student.getFirstName(),
                student.getLastName(),
                student.getIndexNumber(),
                student.getGroupName() != null ? student.getGroupName() : ""
        );
    }
}