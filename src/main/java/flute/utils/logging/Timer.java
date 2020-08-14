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

    public long getTimeCounter() {
        Date newDate = new Date();
        long delta = newDate.getTime() - lastTime.getTime();
        lastTime = newDate;
        return delta;
    }
}
