package com.example.javafxfront;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StudentService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String STUDENTS_ENDPOINT = BASE_URL + "/students";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StudentService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CompletableFuture<List<Student>> getAllStudentsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseStudentsFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac studentow z serwera: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Student>> getStudentsByGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = STUDENTS_ENDPOINT + "/group/" + java.net.URLEncoder.encode(groupName, "UTF-8");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseStudentsFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac studentow grupy z serwera: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<List<Student>> getStudentsWithoutGroupAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = STUDENTS_ENDPOINT + "/without-group";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseStudentsFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie pobrac studentow bez grupy z serwera: " + e.getMessage(), e);
            }
        });
    }

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
                    return parseStudentFromJson(response.body());
                } else if (response.statusCode() == 409) {
                    throw new StudentAlreadyExistsException("Student o numerze indeksu " +
                            student.getIndexNumber() + " już istnieje w systemie!");
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode()
                            + ". Szczegóły: " + response.body());
                }

            } catch (StudentAlreadyExistsException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie dodac studenta na serwer: " + e.getMessage(), e);
            }
        });
    }

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

    public CompletableFuture<Student> updateStudentAsync(String indexNumber, Student student) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = studentToJson(student);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT + "/" + indexNumber))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseStudentFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode());
                }

            } catch (Exception e) {
                throw new RuntimeException("Nie udalo sie zaktualizowac studenta na serwerze: " + e.getMessage(), e);
            }
        });
    }

    // === METODY PRYWATNE DO PARSOWANIA JSON ===

    private List<Student> parseStudentsFromJson(String json) {
        try {
            List<StudentFromServer> serverStudents = objectMapper.readValue(json, new TypeReference<List<StudentFromServer>>() {});

            return serverStudents.stream()
                    .map(this::convertToStudent)
                    .toList();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse students JSON: " + e.getMessage(), e);
        }
    }

    private Student parseStudentFromJson(String json) {
        try {
            StudentFromServer serverStudent = objectMapper.readValue(json, StudentFromServer.class);
            return convertToStudent(serverStudent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse student JSON: " + e.getMessage(), e);
        }
    }

    private String studentToJson(Student student) {
        try {
            StudentToServer studentToServer = new StudentToServer();
            studentToServer.firstName = student.getFirstName();
            studentToServer.lastName = student.getLastName();
            studentToServer.indexNumber = student.getIndexNumber();
            studentToServer.groupName = student.getGroupName();

            return objectMapper.writeValueAsString(studentToServer);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert student to JSON: " + e.getMessage(), e);
        }
    }

    private Student convertToStudent(StudentFromServer serverStudent) {
        String groupName = null;
        if (serverStudent.group != null) {
            groupName = serverStudent.group.name;
        }

        Student student = new Student(serverStudent.firstName, serverStudent.lastName,
                serverStudent.indexNumber, groupName);
        return student;
    }

    // === KLASY POMOCNICZE - BEZ EMAIL ===

    private static class StudentFromServer {
        public Long id;
        public String firstName;
        public String lastName;
        public String indexNumber;
        // USUNIĘTE POLE EMAIL
        public String fullName;
        public LocalDateTime createdDate;
        public Boolean active;
        public GroupInfo group;
    }

    private static class GroupInfo {
        public Long id;
        public String name;
        public String specialization;
    }

    private static class StudentToServer {
        public String firstName;
        public String lastName;
        public String indexNumber;
        public String groupName;
        // BEZ POLA EMAIL
    }

    public static class StudentAlreadyExistsException extends RuntimeException {
        public StudentAlreadyExistsException(String message) {
            super(message);
        }
    }
}