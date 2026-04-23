import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Catalogue {

    private ArrayList<Publication> publications = new ArrayList<>();

    public void addPublication(Publication p) {
        publications.add(p);
    }

    public void removePublicationByTitle(String title) throws BookNotFoundException {
        Publication found = findPublicationByTitle(title);
        publications.remove(found);
    }

    public Publication findPublicationByTitle(String title) throws BookNotFoundException {
        for (Publication p : publications) {
            if (p.getTitle().equalsIgnoreCase(title)) {
                return p;
            }
        }
        throw new BookNotFoundException(title);
    }

    public List<Publication> getAllPublications() {
        return new ArrayList<>(publications);
    }

    public List<Publication> searchByTitle(String query) {
        List<Publication> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Publication p : publications) {
            if (p.getTitle().toLowerCase().contains(lowerQuery)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Publication> searchByAuthor(String query) {
        List<Publication> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Publication p : publications) {
            if (p instanceof Book b && b.getAuthor().toLowerCase().contains(lowerQuery)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Publication> filterByGenre(String genre) {
        List<Publication> result = new ArrayList<>();
        for (Publication p : publications) {
            if (p instanceof Book b && b.getGenre().equalsIgnoreCase(genre)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<String> getDistinctGenres() {
        return publications.stream()
                .filter(p -> p instanceof Book)
                .map(p -> ((Book) p).getGenre())
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public void exportToCSV(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Title,Author,Publisher,Year,Genre");
            for (Publication p : publications) {
                if (p instanceof Book b) {
                    pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                            b.getTitle(), b.getAuthor(), b.getPublisher(), b.getYear(), b.getGenre());
                }
            }
        }
    }

    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(publications);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            publications = (ArrayList<Publication>) ois.readObject();
        }
    }
}
