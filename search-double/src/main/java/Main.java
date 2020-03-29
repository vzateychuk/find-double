import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("No directory defined. Proper use: > java app-name directory-name [Y - if delete duplicates]");
        }

        System.out.println("Application started at: " + LocalDateTime.now());

        Path path = Paths.get(args[0]);
        Map<MyFile, List<Path>> pathsMap =
                Files.walk(path)
                .filter(p -> !Files.isDirectory(p))
                .map(p -> {
                    MyFile file = null;
                    try {
                        BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
                        file = new MyFile(p, a.size(), a.creationTime());
                    } catch (Exception e) {
                        System.out.println("Main.main: ERROR when processing: " + p.toString());
                        e.getStackTrace();
                    }
                    return file;
                }).collect( Collectors.groupingBy(  Function.identity(),
                                                    Collectors.mapping( MyFile::getPath,Collectors.toList() )
                ));

        System.out.println("Total files processed: " + pathsMap.size() + " in folder: '" + path.toString() + '\'');

        boolean deleteDuplicates = args.length > 1 && "y".equalsIgnoreCase(args[1]);

        pathsMap.forEach((p, paths) -> {
            if ( paths.size()>1 ) {
                System.out.println("--> " + p.getPath().getFileName() + ", duplicates: " + paths.size());
                System.out.println(paths.get(0));
                for (int i = 1; i < paths.size(); i++) {
                    System.out.print(paths.get(i));
                    try {
                        if (deleteDuplicates) {
                            Files.delete(paths.get(i));
                            System.out.print( " - DELETED");
                        }
                    } catch (IOException e) {
                        System.out.print(paths.get(i) + " - UNABLE TO DELETE: " + e.getMessage());
                    }
                    System.out.print(System.getProperty("line.separator"));
                }
                System.out.println("<--");
            }
        });


        System.out.println("Application finished at: " + LocalDateTime.now());
    }

    static class MyFile {
        private final Path path;
        private final long size;
        private final FileTime created;

        public MyFile(Path path, long size, FileTime created) {
            if (path == null ) {
                throw new IllegalArgumentException("Path can't be null");
            }
            this.path = path;
            this.size = size;
            this.created = created;
        }

        public Path getPath() {
            return path;
        }
        public long getSize() {
            return size;
        }
        public FileTime getCreated() {
            return created;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyFile other = (MyFile) o;
            String name = path.getFileName().toString();
            return size == other.size &&
                    created.equals(other.created) &&
                    name.equals(other.getPath().getFileName().toString());
        }

        @Override
        public int hashCode() {
            return Objects.hash(path.getFileName().toString(), size, created);
        }


        @Override
        public String toString() {
            return path.getFileName().toString() +
                    "{size=" + size + ", created='" + created + "', path='"+path.toAbsolutePath().toString()+"'}";
        }
    }
}
