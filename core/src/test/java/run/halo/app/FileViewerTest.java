package run.halo.app;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileViewerTest {
    public static void main(String[] args) throws IOException {
//        String file = "F:\\IT\\Code\\halo-plugin-experimental\\build\\plugins\\plugin-potatoes-1.0-SNAPSHOT.zip";
//        ZipFile zipFile = new ZipFile(file);
//        new ZipFile(file).extractFile("classes/static/", "E:\\tmp","cc");
        String file = "F:\\IT\\Code\\halo-plugin-experimental\\plugins\\potatoes";
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/plugin-*.zip");
        Path path = Files.walkFileTree(Paths.get(file), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(file)) {
                    System.out.println(file);
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println(path.getFileName());
    }
}
