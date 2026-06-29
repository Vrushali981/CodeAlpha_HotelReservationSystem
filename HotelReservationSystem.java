import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;

public class HotelReservationSystem {

    // ─── Room ─────────────────────────────────────────────────────────────────
    enum RoomType { STANDARD, DELUXE, SUITE }

    static class Room {
        int      number;
        RoomType type;
        double   pricePerNight;
        boolean  available;

        Room(int number, RoomType type, double pricePerNight) {
            this.number       = number;
            this.type         = type;
            this.pricePerNight = pricePerNight;
            this.available    = true;
        }

        @Override
        public String toString() {
            return String.format("Room %03d | %-8s | $%.2f/night | %s",
                    number, type, pricePerNight, available ? "Available" : "Booked");
        }
    }

    // ─── Reservation ─────────────────────────────────────────────────────────
    static class Reservation {
        static int counter = 1000;

        String   bookingId;
        String   guestName;
        String   guestEmail;
        Room     room;
        LocalDate checkIn;
        LocalDate checkOut;
        double   totalCost;
        boolean  paid;
        String   timestamp;

        Reservation(String guestName, String guestEmail, Room room,
                    LocalDate checkIn, LocalDate checkOut) {
            this.bookingId  = "BK" + (++counter);
            this.guestName  = guestName;
            this.guestEmail = guestEmail;
            this.room       = room;
            this.checkIn    = checkIn;
            this.checkOut   = checkOut;
            long nights     = ChronoUnit.DAYS.between(checkIn, checkOut);
            this.totalCost  = nights * room.pricePerNight;
            this.paid       = false;
            this.timestamp  = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        long nights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }

        void printReceipt() {
            System.out.println("\n╔══════════════ BOOKING CONFIRMATION ══════════════╗");
            System.out.println("  Booking ID  : " + bookingId);
            System.out.println("  Guest       : " + guestName + " (" + guestEmail + ")");
            System.out.println("  Room        : " + room.number + " (" + room.type + ")");
            System.out.println("  Check-In    : " + checkIn);
            System.out.println("  Check-Out   : " + checkOut);
            System.out.println("  Nights      : " + nights());
            System.out.printf ("  Rate        : $%.2f/night%n", room.pricePerNight);
            System.out.printf ("  Total Cost  : $%.2f%n", totalCost);
            System.out.println("  Payment     : " + (paid ? "PAID ✓" : "PENDING"));
            System.out.println("  Booked At   : " + timestamp);
            System.out.println("╚═══════════════════════════════════════════════════╝");
        }

        @Override
        public String toString() {
            return String.format("%s | Room %03d | %s to %s | $%.2f | %s | %s",
                    bookingId, room.number, checkIn, checkOut, totalCost,
                    paid ? "PAID" : "PENDING", guestName);
        }
    }

    // ─── Hotel ────────────────────────────────────────────────────────────────
    private final List<Room>        rooms        = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final Scanner           scanner      = new Scanner(System.in);
    private static final String     DATA_FILE    = "hotel_data.txt";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public HotelReservationSystem() {
        // Seed rooms
        int n = 1;
        for (int i = 0; i < 5; i++) rooms.add(new Room(n++, RoomType.STANDARD, 80.00));
        for (int i = 0; i < 4; i++) rooms.add(new Room(n++, RoomType.DELUXE,   130.00));
        for (int i = 0; i < 3; i++) rooms.add(new Room(n++, RoomType.SUITE,    220.00));
        loadData();
    }

    public void run() {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║    GRAND JAVA HOTEL  v1.0          ║");
        System.out.println("╚════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> viewRooms();
                case 2 -> makeReservation();
                case 3 -> cancelReservation();
                case 4 -> viewReservations();
                case 5 -> searchReservation();
                case 6 -> processPayment();
                case 7 -> { saveData(); System.out.println("Data saved."); }
                case 8 -> { saveData(); running = false; System.out.println("Goodbye!"); }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n─── MENU ───────────────────────");
        System.out.println("1. View Available Rooms");
        System.out.println("2. Make Reservation");
        System.out.println("3. Cancel Reservation");
        System.out.println("4. View All Reservations");
        System.out.println("5. Search Reservation");
        System.out.println("6. Process Payment");
        System.out.println("7. Save Data");
        System.out.println("8. Exit");
        System.out.println("────────────────────────────────");
    }

    private void viewRooms() {
        System.out.println("\n─── ROOM DIRECTORY ─────────────────────────────");
        for (RoomType type : RoomType.values()) {
            System.out.println("\n  [ " + type + " ]");
            rooms.stream()
                 .filter(r -> r.type == type)
                 .forEach(r -> System.out.println("    " + r));
        }
    }

    private void makeReservation() {
        System.out.print("Guest name    : ");
        String name = scanner.nextLine().trim();
        System.out.print("Guest email   : ");
        String email = scanner.nextLine().trim();

        System.out.println("Room type (1=Standard $80 | 2=Deluxe $130 | 3=Suite $220):");
        int typeChoice = getInt("Select: ");
        RoomType type = switch (typeChoice) {
            case 1 -> RoomType.STANDARD;
            case 2 -> RoomType.DELUXE;
            case 3 -> RoomType.SUITE;
            default -> { System.out.println("Invalid type."); yield null; }
        };
        if (type == null) return;

        LocalDate checkIn  = getDate("Check-in  date (yyyy-MM-dd): ");
        LocalDate checkOut = getDate("Check-out date (yyyy-MM-dd): ");
        if (checkIn == null || checkOut == null) return;
        if (!checkOut.isAfter(checkIn)) {
            System.out.println("Check-out must be after check-in."); return;
        }

        // Find available room of that type
        Room chosen = rooms.stream()
                .filter(r -> r.type == type && r.available)
                .findFirst().orElse(null);
        if (chosen == null) {
            System.out.println("No available " + type + " rooms. Try another type."); return;
        }

        Reservation res = new Reservation(name, email, chosen, checkIn, checkOut);
        chosen.available = false;
        reservations.add(res);
        res.printReceipt();

        // Payment prompt
        System.out.print("\nProcess payment now? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            processPayment(res);
        }
    }

    private void cancelReservation() {
        System.out.print("Enter Booking ID to cancel: ");
        String id = scanner.nextLine().trim().toUpperCase();
        Reservation res = findById(id);
        if (res == null) { System.out.println("Booking not found."); return; }
        res.room.available = true;
        reservations.remove(res);
        System.out.println("Reservation " + id + " cancelled. Room " + res.room.number + " is now available.");
    }

    private void viewReservations() {
        if (reservations.isEmpty()) { System.out.println("No reservations found."); return; }
        System.out.println("\n─── ALL RESERVATIONS ────────────────────────────────────────────");
        reservations.forEach(r -> System.out.println("  " + r));
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("Total: " + reservations.size() + " reservation(s)");
    }

    private void searchReservation() {
        System.out.print("Search by (1=Booking ID | 2=Guest name): ");
        int choice = getInt("");
        if (choice == 1) {
            System.out.print("Booking ID: ");
            String id = scanner.nextLine().trim().toUpperCase();
            Reservation r = findById(id);
            if (r == null) System.out.println("Not found.");
            else r.printReceipt();
        } else if (choice == 2) {
            System.out.print("Guest name: ");
            String name = scanner.nextLine().trim().toLowerCase();
            reservations.stream()
                    .filter(r -> r.guestName.toLowerCase().contains(name))
                    .forEach(Reservation::printReceipt);
        }
    }

    private void processPayment() {
        System.out.print("Enter Booking ID: ");
        String id = scanner.nextLine().trim().toUpperCase();
        Reservation res = findById(id);
        if (res == null) { System.out.println("Booking not found."); return; }
        processPayment(res);
    }

    private void processPayment(Reservation res) {
        if (res.paid) { System.out.println("This booking is already paid."); return; }
        System.out.printf("Total due: $%.2f%n", res.totalCost);
        System.out.println("Payment method (1=Credit Card | 2=Cash | 3=UPI):");
        int m = getInt("Select: ");
        String method = switch (m) {
            case 1 -> "Credit Card";
            case 2 -> "Cash";
            case 3 -> "UPI";
            default -> "Unknown";
        };
        res.paid = true;
        System.out.printf("✓ Payment of $%.2f received via %s for Booking %s%n",
                res.totalCost, method, res.bookingId);
    }

    private Reservation findById(String id) {
        return reservations.stream()
                .filter(r -> r.bookingId.equals(id))
                .findFirst().orElse(null);
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    private void saveData() {
        try (PrintWriter pw = new PrintWriter(DATA_FILE)) {
            // Save room availability
            for (Room r : rooms)
                pw.println("ROOM|" + r.number + "|" + r.type + "|" + r.pricePerNight + "|" + r.available);
            // Save reservations
            for (Reservation r : reservations)
                pw.println("RES|" + r.bookingId + "|" + r.guestName + "|" + r.guestEmail
                        + "|" + r.room.number + "|" + r.checkIn + "|" + r.checkOut + "|" + r.paid);
        } catch (IOException e) {
            System.out.println("Save error: " + e.getMessage());
        }
    }

    private void loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p[0].equals("ROOM")) {
                    int num = Integer.parseInt(p[1]);
                    rooms.stream().filter(r -> r.number == num).findFirst()
                         .ifPresent(r -> r.available = Boolean.parseBoolean(p[4]));
                } else if (p[0].equals("RES")) {
                    int roomNum = Integer.parseInt(p[4]);
                    Room room = rooms.stream().filter(r -> r.number == roomNum)
                                    .findFirst().orElse(null);
                    if (room != null) {
                        Reservation res = new Reservation(p[2], p[3], room,
                                LocalDate.parse(p[5]), LocalDate.parse(p[6]));
                        res.bookingId = p[1];
                        res.paid      = Boolean.parseBoolean(p[7]);
                        reservations.add(res);
                    }
                }
            }
            System.out.println("Previous data loaded successfully.");
        } catch (IOException e) {
            System.out.println("No previous data found, starting fresh.");
        }
    }

    private int getInt(String prompt) {
        System.out.print(prompt);
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private LocalDate getDate(String prompt) {
        System.out.print(prompt);
        try { return LocalDate.parse(scanner.nextLine().trim(), DATE_FMT); }
        catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Use yyyy-MM-dd.");
            return null;
        }
    }

    public static void main(String[] args) {
        new HotelReservationSystem().run();
    }
}