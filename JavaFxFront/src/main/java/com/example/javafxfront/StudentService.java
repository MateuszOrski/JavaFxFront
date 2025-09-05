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
import java.util.Optional;

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

                System.out.println("🔗 Wywołuję URL: " + url); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi: " + response.statusCode()); // DEBUG
                System.out.println("📄 Treść odpowiedzi: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    List<Student> students = parseStudentsFromJson(response.body());
                    System.out.println("✅ Sparsowano " + students.size() + " studentów"); // DEBUG
                    return students;
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Treść: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd getStudentsByGroupAsync: " + e.getMessage()); // DEBUG
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
                System.out.println("📤 Wysyłam JSON: " + jsonBody); // DEBUG

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
                String jsonBody = studentToJsonForUpdate(student);
                System.out.println("🔄 Aktualizuję studenta " + indexNumber + " JSON: " + jsonBody); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(STUDENTS_ENDPOINT + "/" + indexNumber))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Update status: " + response.statusCode()); // DEBUG
                System.out.println("📄 Update response: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    return parseStudentFromJson(response.body());
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Treść: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd updateStudentAsync: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udalo sie zaktualizowac studenta na serwerze: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Student> removeStudentFromGroupAsync(String indexNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = STUDENTS_ENDPOINT + "/remove-from-group/" + java.net.URLEncoder.encode(indexNumber, "UTF-8");

                System.out.println("🔗 Wywołuję URL usuwania z grupy: " + url); // DEBUG

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.ofString("{}"))  // Pusty body dla PUT
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📡 Status odpowiedzi usuwania z grupy: " + response.statusCode()); // DEBUG
                System.out.println("📄 Treść odpowiedzi: " + response.body()); // DEBUG

                if (response.statusCode() == 200) {
                    Student updatedStudent = parseStudentFromJson(response.body());
                    System.out.println("✅ Student usunięty z grupy: " + updatedStudent.getFullName() +
                            " (grupa: " + (updatedStudent.getGroupName() != null ? updatedStudent.getGroupName() : "BRAK") + ")");
                    return updatedStudent;
                } else {
                    throw new RuntimeException("Serwer odpowiedzial statusem: " + response.statusCode() +
                            ". Treść: " + response.body());
                }

            } catch (Exception e) {
                System.err.println("❌ Błąd removeStudentFromGroupAsync: " + e.getMessage()); // DEBUG
                throw new RuntimeException("Nie udalo sie usunac studenta z grupy na serwerze: " + e.getMessage(), e);
            }
        });
    }

    // NOWA METODA - Specjalna konwersja dla aktualizacji z nazwą grupy
    private String studentToJsonForUpdate(Student student) {
        try {
            // Tworzymy JSON który backend może zrozumieć
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"firstName\":\"").append(escapeJson(student.getFirstName())).append("\",");
            json.append("\"lastName\":\"").append(escapeJson(student.getLastName())).append("\",");
            json.append("\"indexNumber\":\"").append(escapeJson(student.getIndexNumber())).append("\"");

            // KLUCZOWE: Jeśli student ma grupę, dodaj ją jako obiekt Group
            if (student.getGroupName() != null && !student.getGroupName().trim().isEmpty()) {
                json.append(",\"group\":{\"name\":\"").append(escapeJson(student.getGroupName())).append("\"}");
            } else {
                json.append(",\"group\":null");
            }

            json.append("}");
            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert student to JSON for update: " + e.getMessage(), e);
        }
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
            groupName = serverStudent.group.name; // Pobierz nazwę z obiektu grupy
        }

        Student student = new Student(serverStudent.firstName, serverStudent.lastName,
                serverStudent.indexNumber, groupName);

        // DEBUG - pokaż co zostało sparsowane
        System.out.println("🔄 Sparsowano studenta: " + student.getFullName() +
                " (grupa: " + (groupName != null ? groupName : "BRAK") + ")");

        return student;
    }




    /**
     * DODANA BRAKUJĄCA METODA - Escape'owanie stringów w JSON
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // === KLASY POMOCNICZE - BEZ EMAIL ===

    private static class StudentFromServer {
        public Long id;
        public String firstName;
        public String lastName;
        public String indexNumber;
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

    public Student removeStudentFromGroup(String indexNumber) {
        System.out.println("=== STUDENT SERVICE: removeStudentFromGroup ===");
        System.out.println("🔍 Szukam studenta o indeksie: " + indexNumber);

        Optional<Student> studentOpt = studentRepository.findByIndexNumber(indexNumber);

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            String previousGroup = student.getGroup() != null ? student.getGroup().getName() : "BRAK";

            System.out.println("📋 Znaleziono studenta: " + student.getFullName());
            System.out.println("📋 Aktualna grupa: " + previousGroup);

            // KLUCZOWE: Usuń przypisanie do grupy
            student.setGroup(null);

            // Zapisz zmiany w bazie danych
            Student updatedStudent = studentRepository.save(student);

            System.out.println("✅ Student " + updatedStudent.getFullName() + " został usunięty z grupy: " + previousGroup);
            System.out.println("✅ Nowy status grupy: " + (updatedStudent.getGroup() != null ? updatedStudent.getGroup().getName() : "BRAK"));

            return updatedStudent;
        } else {
            System.err.println("❌ Student o indeksie " + indexNumber + " nie został znaleziony w bazie");
            throw new RuntimeException("Student not found with index: " + indexNumber);
        }
    }

}