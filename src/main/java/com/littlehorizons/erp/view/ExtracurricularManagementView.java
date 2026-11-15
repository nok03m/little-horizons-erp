package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.time.LocalDate;
import java.util.List;

/**
 * Console UI for clubs and school events.
 */
public class ExtracurricularManagementView {

    private static final List<String> MEMBERSHIP_STATUSES = List.of("Activa", "Retirada");
    private static final List<String> EVENT_TYPES = List.of("Academico", "Cultural", "Deportivo", "Social", "Cientifico");
    private static final List<String> PARTICIPATION_ROLES = List.of("Participante", "Competidor", "Organizador", "Asistente");
    private static final List<String> POSITIONS = List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");

    private final ConsoleHelper console;
    private final ExtracurricularController extracurricularController;
    private final StudentController studentController;
    private final TeacherController teacherController;

    public ExtracurricularManagementView(ConsoleHelper console,
                                         ExtracurricularController extracurricularController,
                                         StudentController studentController,
                                         TeacherController teacherController) {
        this.console = console;
        this.extracurricularController = extracurricularController;
        this.studentController = studentController;
        this.teacherController = teacherController;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Extracurricular Activities ---");
            System.out.println("1. Clubs");
            System.out.println("2. Club memberships");
            System.out.println("3. Events");
            System.out.println("4. Event participants");
            System.out.println("5. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 5);
            switch (option) {
                case 1 -> showClubs();
                case 2 -> showMemberships();
                case 3 -> showEvents();
                case 4 -> showParticipants();
                case 5 -> running = false;
                default -> console.printError("Unexpected option.");
            }
        }
    }

    private void showClubs() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Clubs ---");
            System.out.println("1. List all");
            System.out.println("2. View by ID");
            System.out.println("3. Create");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        extracurricularController.listClubs().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int id = console.readPositiveInt("Club ID: ");
                        extracurricularController.getClub(id).ifPresentOrElse(club -> {
                            printClub(club);
                            System.out.println("Members:");
                            List<ClubMembership> members = extracurricularController.listMembershipsByClub(id);
                            if (members.isEmpty()) {
                                System.out.println("  (none)");
                            } else {
                                members.forEach(member -> System.out.println("  " + member));
                            }
                        }, () -> console.printError("Club not found."));
                        console.pause();
                    }
                    case 3 -> {
                        Club club = readClub();
                        int id = extracurricularController.createClub(club);
                        console.printSuccess("Club created with ID " + id);
                        console.pause();
                    }
                    case 4 -> updateClub();
                    case 5 -> {
                        int id = console.readPositiveInt("Club ID: ");
                        if (console.confirm("Delete club?")) {
                            if (extracurricularController.deleteClub(id)) {
                                console.printSuccess("Club deleted.");
                            } else {
                                console.printError("Club not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Clubs menu failed", exception);
                console.pause();
            }
        }
    }

    private void updateClub() {
        int id = console.readPositiveInt("Club ID: ");
        extracurricularController.getClub(id).ifPresentOrElse(existing -> {
            printClub(existing);
            Club updated = readClub();
            updated.setId(id);
            if (extracurricularController.updateClub(updated)) {
                console.printSuccess("Club updated.");
            } else {
                console.printError("Club was not updated.");
            }
        }, () -> console.printError("Club not found."));
        console.pause();
    }

    private Club readClub() {
        Club club = new Club();
        club.setName(console.readRequired("Club name: "));
        club.setDescription(console.readOptional("Description (optional): "));
        if (console.confirm("Assign supervisor teacher?")) {
            int teacherId = console.readPositiveInt("Teacher ID: ");
            if (teacherController.getTeacher(teacherId).isEmpty()) {
                throw new IllegalArgumentException("Teacher not found.");
            }
            club.setSupervisorTeacherId(teacherId);
        }
        club.setMaxCapacity(console.readIntInRange("Max capacity: ", 1, 500));
        club.setMeetingSchedule(console.readOptional("Meeting schedule (optional): "));
        club.setActive(console.confirm("Is active?"));
        return club;
    }

    private void printClub(Club club) {
        System.out.println("ID: " + club.getId());
        System.out.println("Name: " + club.getName());
        System.out.println("Description: " + nullToDash(club.getDescription()));
        System.out.println("Supervisor: " + nullToDash(club.getSupervisorTeacherName()));
        System.out.println("Capacity: " + club.getActiveMembers() + "/" + club.getMaxCapacity());
        System.out.println("Schedule: " + nullToDash(club.getMeetingSchedule()));
        System.out.println("Active: " + club.isActive());
    }

    private void showMemberships() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Club Memberships ---");
            System.out.println("1. List all");
            System.out.println("2. List by club");
            System.out.println("3. List by student");
            System.out.println("4. Enroll student (validates capacity)");
            System.out.println("5. Update / withdraw");
            System.out.println("6. Delete");
            System.out.println("7. Back");
            int option = console.readIntInRange("Select an option: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> {
                        extracurricularController.listMemberships().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int clubId = console.readPositiveInt("Club ID: ");
                        extracurricularController.listMembershipsByClub(clubId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        extracurricularController.listMembershipsByStudent(studentId).forEach(System.out::println);
                        console.pause();
                    }
                    case 4 -> enrollStudent();
                    case 5 -> updateMembership();
                    case 6 -> {
                        int id = console.readPositiveInt("Membership ID: ");
                        if (console.confirm("Delete membership?")) {
                            if (extracurricularController.deleteMembership(id)) {
                                console.printSuccess("Membership deleted.");
                            } else {
                                console.printError("Membership not found.");
                            }
                        }
                        console.pause();
                    }
                    case 7 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Memberships menu failed", exception);
                console.pause();
            }
        }
    }

    private void enrollStudent() {
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return;
        }
        extracurricularController.listClubs().forEach(System.out::println);
        int clubId = console.readPositiveInt("Club ID: ");
        ClubMembership membership = new ClubMembership();
        membership.setStudentId(studentId);
        membership.setClubId(clubId);
        membership.setJoinDate(console.readDate("Join date"));
        membership.setStatus("Activa");
        int id = extracurricularController.enrollInClub(membership);
        console.printSuccess("Membership created with ID " + id);
        console.pause();
    }

    private void updateMembership() {
        int id = console.readPositiveInt("Membership ID: ");
        extracurricularController.getMembership(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            existing.setStatus(console.readChoice("Status:", MEMBERSHIP_STATUSES));
            if ("Retirada".equals(existing.getStatus())) {
                LocalDate withdrawal = console.readOptionalDate("Withdrawal date");
                existing.setWithdrawalDate(withdrawal == null ? LocalDate.now() : withdrawal);
                existing.setWithdrawalReason(console.readOptional("Withdrawal reason (optional): "));
            } else {
                existing.setWithdrawalDate(null);
                existing.setWithdrawalReason(null);
            }
            if (extracurricularController.updateMembership(existing)) {
                console.printSuccess("Membership updated.");
            } else {
                console.printError("Membership was not updated.");
            }
        }, () -> console.printError("Membership not found."));
        console.pause();
    }

    private void showEvents() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Events ---");
            System.out.println("1. List all");
            System.out.println("2. View by ID");
            System.out.println("3. Create");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        extracurricularController.listEvents().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int id = console.readPositiveInt("Event ID: ");
                        extracurricularController.getEvent(id).ifPresentOrElse(event -> {
                            printEvent(event);
                            System.out.println("Participants:");
                            List<EventParticipant> participants = extracurricularController.listParticipantsByEvent(id);
                            if (participants.isEmpty()) {
                                System.out.println("  (none)");
                            } else {
                                participants.forEach(participant -> System.out.println("  " + participant));
                            }
                        }, () -> console.printError("Event not found."));
                        console.pause();
                    }
                    case 3 -> {
                        SchoolEvent event = readEvent();
                        int id = extracurricularController.createEvent(event);
                        console.printSuccess("Event created with ID " + id);
                        console.pause();
                    }
                    case 4 -> updateEvent();
                    case 5 -> {
                        int id = console.readPositiveInt("Event ID: ");
                        if (console.confirm("Delete event?")) {
                            if (extracurricularController.deleteEvent(id)) {
                                console.printSuccess("Event deleted.");
                            } else {
                                console.printError("Event not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Events menu failed", exception);
                console.pause();
            }
        }
    }

    private void updateEvent() {
        int id = console.readPositiveInt("Event ID: ");
        extracurricularController.getEvent(id).ifPresentOrElse(existing -> {
            printEvent(existing);
            SchoolEvent updated = readEvent();
            updated.setId(id);
            if (extracurricularController.updateEvent(updated)) {
                console.printSuccess("Event updated.");
            } else {
                console.printError("Event was not updated.");
            }
        }, () -> console.printError("Event not found."));
        console.pause();
    }

    private SchoolEvent readEvent() {
        SchoolEvent event = new SchoolEvent();
        event.setName(console.readRequired("Event name: "));
        event.setDescription(console.readOptional("Description (optional): "));
        event.setEventType(console.readChoice("Event type:", EVENT_TYPES));
        event.setEventDate(console.readDate("Event date"));
        event.setLocation(console.readOptional("Location (optional): "));
        event.setHasCompetition(console.confirm("Has competition?"));
        if (console.confirm("Assign organizer teacher?")) {
            int teacherId = console.readPositiveInt("Teacher ID: ");
            if (teacherController.getTeacher(teacherId).isEmpty()) {
                throw new IllegalArgumentException("Teacher not found.");
            }
            event.setOrganizerTeacherId(teacherId);
        }
        event.setActive(console.confirm("Is active?"));
        return event;
    }

    private void printEvent(SchoolEvent event) {
        System.out.println("ID: " + event.getId());
        System.out.println("Name: " + event.getName());
        System.out.println("Type: " + event.getEventType());
        System.out.println("Date: " + event.getEventDate());
        System.out.println("Location: " + nullToDash(event.getLocation()));
        System.out.println("Competition: " + event.isHasCompetition());
        System.out.println("Organizer: " + nullToDash(event.getOrganizerTeacherName()));
        System.out.println("Active: " + event.isActive());
        System.out.println("Description: " + nullToDash(event.getDescription()));
    }

    private void showParticipants() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Event Participants ---");
            System.out.println("1. List all");
            System.out.println("2. List by event");
            System.out.println("3. List by student");
            System.out.println("4. Register participant");
            System.out.println("5. Update (role/position)");
            System.out.println("6. Delete");
            System.out.println("7. Back");
            int option = console.readIntInRange("Select an option: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> {
                        extracurricularController.listParticipants().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int eventId = console.readPositiveInt("Event ID: ");
                        extracurricularController.listParticipantsByEvent(eventId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        extracurricularController.listParticipantsByStudent(studentId).forEach(System.out::println);
                        console.pause();
                    }
                    case 4 -> registerParticipant();
                    case 5 -> updateParticipant();
                    case 6 -> {
                        int id = console.readPositiveInt("Participation ID: ");
                        if (console.confirm("Delete participation?")) {
                            if (extracurricularController.deleteParticipant(id)) {
                                console.printSuccess("Participation deleted.");
                            } else {
                                console.printError("Participation not found.");
                            }
                        }
                        console.pause();
                    }
                    case 7 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Participants menu failed", exception);
                console.pause();
            }
        }
    }

    private void registerParticipant() {
        extracurricularController.listEvents().stream().limit(20).forEach(System.out::println);
        int eventId = console.readPositiveInt("Event ID: ");
        SchoolEvent event = extracurricularController.getEvent(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            throw new IllegalArgumentException("Student not found.");
        }
        EventParticipant participant = new EventParticipant();
        participant.setEventId(eventId);
        participant.setStudentId(studentId);
        participant.setParticipationRole(console.readChoice("Role:", PARTICIPATION_ROLES));
        if (event.isHasCompetition() && console.confirm("Set competition position?")) {
            participant.setPositionObtained(console.readChoice("Position:", POSITIONS));
        }
        participant.setObservations(console.readOptional("Observations (optional): "));
        int id = extracurricularController.registerParticipant(participant);
        console.printSuccess("Participant registered with ID " + id);
        console.pause();
    }

    private void updateParticipant() {
        int id = console.readPositiveInt("Participation ID: ");
        extracurricularController.getParticipant(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            existing.setParticipationRole(console.readChoice("Role:", PARTICIPATION_ROLES));
            if (console.confirm("Set/update competition position?")) {
                existing.setPositionObtained(console.readChoice("Position:", POSITIONS));
            }
            existing.setObservations(console.readOptional("Observations (optional): "));
            if (extracurricularController.updateParticipant(existing)) {
                console.printSuccess("Participation updated.");
            } else {
                console.printError("Participation was not updated.");
            }
        }, () -> console.printError("Participation not found."));
        console.pause();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
