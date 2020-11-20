package flute.utils.logging;

import java.util.Date;

public class Timer {
    private Date lastTime = new Date();

    public Date getCurrentTime() {
        return new Date();
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void startCounter() {
        lastTime = new Date();
    }

    //setup new time counter
    public long getTimeCounter() {
        Date newDate = new Date();
        long delta = newDate.getTime() - lastTime.getTime();
        lastTime = newDate;
        return delta;
    }

    public static String formatTime(long second) {
        long hours = second / 3600;
        long minutes = (second % 3600) / 60;
        long seconds = second % 60;
        if (hours > 0) {
            return String.format("%02dh%02dm%02ds", hours, minutes, seconds);
        } else {
            return String.format("%02dm%02ds", minutes, seconds);

        }
    }
}
