import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PackageMigrator {

    private static final String[] MODULE_ROOTS = {
            "riftt-core/src/main/java",
            "riftt-desktop/src/main/java"
    };

    public static void main(String[] args) {
        System.out.println("Starting Package Migration to com.sunny.riftt...");

        try {
            for (String root : MODULE_ROOTS) {
                migrateModule(Paths.get(root));
            }
            System.out.println("Migration completed successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void migrateModule(Path sourceRoot) throws IOException {
        Path sunnyPackage = sourceRoot.resolve("com/sunny");
        Path rifttPackage = sunnyPackage.resolve("riftt");

        if (!Files.exists(sunnyPackage)) {
            System.out.println("Skipping " + sourceRoot + " (com/sunny not found)");
            return;
        }

        // 1. Create 'riftt' directory
        if (!Files.exists(rifttPackage)) {
            Files.createDirectories(rifttPackage);
        }

        // 2. Move all children of com/sunny into com/sunny/riftt (excluding 'riftt'
        // itself)
        try (var stream = Files.list(sunnyPackage)) {
            List<Path> filesToMove = stream
                    .filter(p -> !p.getFileName().toString().equals("riftt"))
                    .collect(Collectors.toList());

            for (Path path : filesToMove) {
                Path dest = rifttPackage.resolve(path.getFileName());
                System.out.println("Moving " + path + " -> " + dest);
                Files.move(path, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // 3. Update File Content
        Files.walkFileTree(rifttPackage, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    updateJavaFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void updateJavaFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<String> newLines = new ArrayList<>();
        boolean modified = false;

        for (String line : lines) {
            String original = line;

            // Update package declaration
            if (line.trim().startsWith("package com.sunny")) {
                line = line.replace("package com.sunny", "package com.sunny.riftt");
            }

            // Update imports
            if (line.trim().startsWith("import com.sunny.")) {
                line = line.replace("import com.sunny.", "import com.sunny.riftt.");
            }

            if (!line.equals(original)) {
                modified = true;
            }
            newLines.add(line);
        }

        if (modified) {
            System.out.println("Updating " + file);
            Files.write(file, newLines);
        }
    }
}
