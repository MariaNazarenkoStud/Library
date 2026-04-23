import java.io.Serializable;

public class Publication implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private int year;

    public Publication(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "Publication{title='" + title + "', year=" + year + "}";
    }
}
