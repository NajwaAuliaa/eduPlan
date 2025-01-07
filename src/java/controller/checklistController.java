package controller;

import model.ListTopic;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/checklist") // Menentukan URL mapping untuk servlet ini
public class checklistController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action"); // Mendapatkan parameter "action" dari request

        // Jika aksi adalah "clearAndRedirect", hapus semua data dan redirect ke halaman "schedule"
        if ("clearAndRedirect".equals(action)) {
            ListTopic.getAllTopics().clear(); // Menghapus semua topik dari model
            HttpSession session = request.getSession(); // Mendapatkan session pengguna
            session.removeAttribute("timeLeft"); // Menghapus atribut waktu tersisa
            session.removeAttribute("timerRunning"); // Menghapus status timer
            session.removeAttribute("currentActivityIndex"); // Menghapus indeks aktivitas saat ini
            session.setAttribute("activitiesStarted", false); // Menyetel bahwa aktivitas belum dimulai
            response.sendRedirect("schedule"); // Redirect ke halaman "schedule"
            return;
        }

        HttpSession session = request.getSession(); // Mendapatkan sesi pengguna
        Integer timeLeft = (Integer) session.getAttribute("timeLeft"); // Mendapatkan waktu tersisa dari sesi
        Boolean timerRunning = (Boolean) session.getAttribute("timerRunning"); // Mendapatkan status timer
        Integer currentTopicIndex = (Integer) session.getAttribute("currentActivityIndex"); // Mendapatkan indeks topik saat ini

        // Jika atribut-atribut tidak ada, tetapkan nilai default
        if (timeLeft == null) {
            timeLeft = 0;
            session.setAttribute("timeLeft", timeLeft);
        }
        if (timerRunning == null) {
            timerRunning = false;
            session.setAttribute("timerRunning", timerRunning);
        }
        if (currentTopicIndex == null) {
            currentTopicIndex = 0;
            session.setAttribute("currentActivityIndex", currentTopicIndex);
        }

        List<ListTopic> topics = ListTopic.getAllTopics(); // Mendapatkan semua topik dari model
        request.setAttribute("activities", topics); // Menyimpan topik-topik dalam atribut request
        request.setAttribute("timeLeft", timeLeft); // Menyimpan waktu tersisa dalam atribut request
        request.setAttribute("timerRunning", timerRunning); // Menyimpan status timer dalam atribut request

        String requestedWith = request.getHeader("X-Requested-With"); // Mengecek apakah request adalah AJAX
        boolean isAjax = "XMLHttpRequest".equals(requestedWith);
        if (isAjax) { // Jika request adalah AJAX, kembalikan JSON
            response.setContentType("application/json");
            response.getWriter().write("{" +
                    "\"timeLeft\": " + timeLeft + ", " +
                    "\"timerRunning\": " + timerRunning +
                    "}");
        } else { // Jika bukan, forward ke halaman checklist.jsp
            request.getRequestDispatcher("/checklist.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action"); // Mendapatkan parameter "action" dari request
        HttpSession session = request.getSession(); // Mendapatkan sesi pengguna

        if ("addActivity".equals(action)) { // Jika aksi adalah menambahkan aktivitas
            Boolean topicsStarted = (Boolean) session.getAttribute("activitiesStarted"); // Mengecek apakah aktivitas sudah dimulai
            if (topicsStarted != null && topicsStarted) {
                response.sendRedirect("checklist"); // Redirect jika aktivitas sudah dimulai
                return;
            }
            String topicName = request.getParameter("activityName"); // Mendapatkan nama aktivitas dari request
            int duration = Integer.parseInt(request.getParameter("duration")); // Mendapatkan durasi dari request
            ListTopic.addTopic(topicName, duration); // Menambahkan aktivitas ke model
        }

        if ("startTimer".equals(action)) { // Jika aksi adalah memulai timer
            List<ListTopic> allTopics = ListTopic.getAllTopics().stream()
                .filter(activity -> !activity.isCompleted()) // Memfilter aktivitas yang belum selesai
                .collect(Collectors.toList());

            if (!allTopics.isEmpty()) { // Jika ada aktivitas yang belum selesai
                session.setAttribute("timeLeft", allTopics.get(0).getDuration() * 60); // Tetapkan waktu untuk aktivitas pertama
                session.setAttribute("timerRunning", true); // Mulai timer
                session.setAttribute("currentActivityIndex", 0); // Tetapkan indeks ke aktivitas pertama
                session.setAttribute("activitiesStarted", true); // Tandai aktivitas sebagai sudah dimulai
                for (ListTopic topic : allTopics) {
                    topic.setChecked(true); // Tandai semua aktivitas yang difilter sebagai dipilih
                }
            }
        }

        if ("nextActivity".equals(action)) { // Jika aksi adalah melanjutkan ke aktivitas berikutnya
            List<ListTopic> checkedTopics = ListTopic.getCheckedTopics(); // Mendapatkan semua aktivitas yang sudah dipilih
            Integer currentTopicIndex = (Integer) session.getAttribute("currentActivityIndex"); // Mendapatkan indeks aktivitas saat ini

            if (currentTopicIndex == null) { // Jika indeks belum ada, tetapkan ke 0
                currentTopicIndex = 0;
            }

            if (currentTopicIndex < checkedTopics.size()) {
                checkedTopics.get(currentTopicIndex).setCompleted(true); // Tandai aktivitas saat ini sebagai selesai
            }

            if (currentTopicIndex + 1 < checkedTopics.size()) { // Jika masih ada aktivitas berikutnya
                session.setAttribute("currentActivityIndex", currentTopicIndex + 1); // Tetapkan indeks ke aktivitas berikutnya
                session.setAttribute("timeLeft", checkedTopics.get(currentTopicIndex + 1).getDuration() * 60); // Tetapkan waktu untuk aktivitas berikutnya
            } else { // Jika tidak ada aktivitas berikutnya
                session.setAttribute("timerRunning", false); // Matikan timer
                session.setAttribute("timeLeft", 0); // Tetapkan waktu tersisa ke 0
            }
        }
        response.sendRedirect("checklist"); // Redirect ke halaman checklist
    }
}
