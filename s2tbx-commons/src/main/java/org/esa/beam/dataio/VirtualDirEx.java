package org.esa.beam.dataio;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarHeader;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by kraftek on 11/24/2014.
 */
public abstract class VirtualDirEx extends VirtualDir {

    private final static HashSet<String> compressedExtensions = new HashSet<String>() {{
        add(".zip");
        add(".tgz");
        add(".gz");
        add(".z");
        add(".tar");
        add(".bz");
        add(".lzh");
        add(".tbz");
    }};

    public static VirtualDirEx create(File file) {
        if (file.isFile() && (TarVirtualDir.isTgz(file.getName()) || TarVirtualDir.isTar(file.getName()))) {
            try {
                return new TarVirtualDir(file);
            } catch (IOException ignored) {
                return null;
            }
        } else {
            return new VirtualDirWrapper(VirtualDir.create(file));
        }
    }

    public static boolean isPackedFile(File file) {
        String extension = FileUtils.getExtension(file);
        return !StringUtils.isNullOrEmpty(extension) && compressedExtensions.contains(extension.toLowerCase());
    }

    public static boolean isTar(String filename) {
        return TarVirtualDir.isTar(filename);
    }

    public String findFirst(String pattern) throws IOException {
        String found = null;
        String[] entries = list("");
        if (entries != null) {
            for (String entry : entries) {
                if (entry.toLowerCase().contains(pattern)) {
                    found = entry;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Tries to findFirst the first file that contains the given string.
     *
     * @param pattern A string to be found in the file name (if any).
     * @return The name of the found file, or <code>NULL</code> if no file was found.
     * @throws IOException
     */
    public String[] findAll(String pattern) throws IOException {
        List<String> found = new ArrayList<String>();
        String[] entries = listAll(); //wrappedVirtualDir.list("");
        if (entries != null) {
            for (String entry : entries) {
                if (entry.toLowerCase().contains(pattern)) {
                    found.add(entry);
                }
            }
        }
        return found.toArray(new String[found.size()]);
    }

    public String[] listAll() {
        String path = getBasePath();
        if (TarVirtualDir.isTar(path) || TarVirtualDir.isTgz(path)) {
            return ((TarVirtualDir) this).listAll();
        } else {
            List<String> fileNames = new ArrayList<String>();
            if (this.isArchive()) {
                try {
                    ZipFile zipFile = new ZipFile(path);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry zipEntry = entries.nextElement();
                        String fileName = zipEntry.getName();//.toLowerCase();
                        if (isTar(fileName)) {
                            File file = getFile(fileName);
                            TarVirtualDir innerTar = new TarVirtualDir(file) {
                                @Override
                                public void close() {
                                }
                            };
                            innerTar.ensureUnpacked(getTempDir());
                            String[] tarFiles = innerTar.listAll();
                            for (String tarFile : tarFiles)
                                fileNames.add(tarFile);
                            file.delete();
                        } else {
                            fileNames.add(fileName);
                        }
                    }
                    zipFile.close();
                } catch (IOException e) {
                    // cannot open zip, list will be empty
                    BeamLogManager.getSystemLogger().severe(e.getMessage());
                }
            } else {
                listFiles(new File(path), fileNames);
            }
            return fileNames.toArray(new String[fileNames.size()]);
        }
    }

    private void listFiles(File parent, List<String> outList) {
        if (parent.isFile())
            return;
        File[] files = parent.listFiles();
        for (File file : files) {
            if (file.isFile())
                outList.add(new File(getBasePath()).toURI().relativize(file.toURI()).getPath().toLowerCase());
            else {
                listFiles(file, outList);
            }
        }
    }

    private static class VirtualDirWrapper extends VirtualDirEx {

        private VirtualDir wrapped;
        protected Map<String, String> files;

        public VirtualDirWrapper(VirtualDir dir) {
            wrapped = dir;
            files = new HashMap<String, String>();
        }

        @Override
        public String getBasePath() {
            return wrapped.getBasePath();
        }

        @Override
        public InputStream getInputStream(String s) throws IOException {
            return wrapped.getInputStream(s);
        }

        @Override
        public File getFile(String relativePath) throws IOException {
            File file = null;
            try {
                file = wrapped.getFile(relativePath);
            } catch (IOException e) {
                file = new File(wrapped.getTempDir(), relativePath);
            }
            if (file == null || !file.exists()) {
                String key = FileUtils.getFileNameFromPath(relativePath).toLowerCase();
                String path = findKeyFile(key);
                if (path == null)
                    throw new IOException(String.format("File %s does not exist", relativePath));
                relativePath = path;
                try {
                    // the "classic" way
                    file = getFileInner(relativePath);
                } catch (IOException e) {
                    file = !isArchive() ? new File(wrapped.getTempDir() + File.separator + relativePath) : getFileInner(relativePath);
                }
            }
            return file;
        }

        private File getFileInner(String s) throws IOException {
            String pathSeparator;
            if (!wrapped.isArchive() && !wrapped.getBasePath().toLowerCase().endsWith("tar")) {
                pathSeparator = "\\\\";
                s = s.replaceAll("/", "\\\\");
            } else {
                pathSeparator = "/";
            }
            try {
                //if the path letter case is correct, there is no need to read all the path tree
                File result = wrapped.getFile(s);
                if (result != null) {
                    return result;
                }
            } catch (IOException ex) {
            }
            String[] relativePathArray = s.split(pathSeparator);
            String newRelativePath = "";
            String[] files = wrapped.list("");
            int index = 0;
            while (files != null && files.length > 0 && index < relativePathArray.length) {
                boolean found = false;
                for (String file : files) {
                    if (relativePathArray[index].equalsIgnoreCase(file)) {
                        newRelativePath += file + pathSeparator;
                        index++;
                        found = true;
                        if (index < relativePathArray.length) {//there are still subfolders/subfiles to be searched
                            files = wrapped.list(newRelativePath);
                        }
                        break;
                    }
                }
                if (!found) {//if no subfolder/subfile did not matched the search, it makes no sense to continue searching
                    break;
                }
            }
            if (index > 0) {//if the file was found (meaning the index is not 0), then the last path separator should be removed!
                newRelativePath = newRelativePath.substring(0, newRelativePath.length() - pathSeparator.length());
            }
            if (index == 0) {
                throw new IOException();
            }
            return wrapped.getFile(newRelativePath);
        }

        @Override
        public String[] list(String s) throws IOException {
            return wrapped.list(s);
        }

        @Override
        public boolean exists(String s) {
            return wrapped.exists(s);
        }

        @Override
        public void close() {
            wrapped.close();
        }

        @Override
        public boolean isCompressed() {
            return wrapped.isCompressed();
        }

        @Override
        public boolean isArchive() {
            return wrapped.isArchive();
        }

        @Override
        public File getTempDir() throws IOException {
            return wrapped.getTempDir();
        }

        @Override
        protected void finalize() throws Throwable {
            wrapped = null;
        }

        @Override
        public String[] listAll() {
            String[] list = super.listAll();
            for (String item : list)
                files.put(FileUtils.getFileNameFromPath(item).toLowerCase(), item);
            return list;
        }

        private String findKeyFile(String key) {
            if (key == null || key.isEmpty())
                return null;
            String ret = files.get(key);
            if (ret == null) {
                Iterator<String> iterator = files.keySet().iterator();
                String namePart = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(key));
                String extPart = FileUtils.getExtension(key);
                while (iterator.hasNext()) {
                    String current = iterator.next();
                    String name = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(current));
                    name = name.substring(name.lastIndexOf("/") + 1);
                    String ext = FileUtils.getExtension(current);
                    if (extPart.equalsIgnoreCase(ext) &&
                            namePart.startsWith(name)) {
                        ret = files.get(current);
                        break;
                    }
                }
            }
            return ret;
        }
    }

    private static class TarVirtualDir extends VirtualDirEx {

        public static final byte LF_SPEC_LINK = (byte) 'L';

        private final File archiveFile;
        private File extractDir;
        private FutureTask<Void> unpackTask;
        private ExecutorService executor;
        private boolean unpackStarted = false;

        private class UnpackProcess implements Callable<Void> {

            @Override
            public Void call() throws Exception {
                ensureUnpacked();
                return null;
            }
        }

        public TarVirtualDir(File tgz) throws IOException {
            if (tgz == null) {
                throw new IllegalArgumentException("Input file shall not be null");
            }
            archiveFile = tgz;
            extractDir = null;
            unpackTask = new FutureTask<Void>(new UnpackProcess());
            executor = Executors.newSingleThreadExecutor();
            //executor.execute(unpackTask);
        }

        public static String getFilenameFromPath(String path) {
            int lastSepIndex = path.lastIndexOf("/");
            if (lastSepIndex == -1) {
                lastSepIndex = path.lastIndexOf("\\");
                if (lastSepIndex == -1) {
                    return path;
                }
            }

            return path.substring(lastSepIndex + 1, path.length());
        }

        public static boolean isTgz(String filename) {
            final String extension = FileUtils.getExtension(filename);
            return (".tgz".equals(extension) || ".gz".equals(extension));
        }

        public static boolean isTar(String filename) {
            return ".tar".equals(FileUtils.getExtension(filename));
        }

        @Override
        public String getBasePath() {
            return archiveFile.getPath();
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            final File file = getFile(path);
            return new BufferedInputStream(new FileInputStream(file));
        }

        @Override
        public File getFile(String path) throws IOException {
            //ensureUnpacked();
            ensureUnpackedStarted();
            try {
                while (!unpackTask.isDone()) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                // swallowed exception
            } finally {
                executor.shutdown();
            }
            final File file = new File(extractDir, path);
            if (!(file.isFile() || file.isDirectory())) {
                throw new IOException();
            }
            return file;
        }

        @Override
        public String[] list(String path) throws IOException {
            final File file = getFile(path);
            return file.list();
        }

        public boolean exists(String s) {
            return archiveFile.exists();
        }

        @Override
        public void close() {
            if (extractDir != null) {
                FileUtils.deleteTree(extractDir);
                extractDir = null;
            }
        }

        @Override
        public boolean isCompressed() {
            return isTgz(archiveFile.getName());
        }

        @Override
        public boolean isArchive() {
            return isTgz(archiveFile.getName());
        }

        @Override
        public void finalize() throws Throwable {
            super.finalize();
            close();
        }

        @Override
        public File getTempDir() throws IOException {
            ensureUnpackedStarted();
            return extractDir;
        }

        public void ensureUnpacked() throws IOException {
            ensureUnpacked(null);
        }

        public void ensureUnpacked(File unpackFolder) throws IOException {
            if (extractDir == null) {
                BeamLogManager.getSystemLogger().info("Unpacking archive contents");
                extractDir = unpackFolder != null ? unpackFolder : VirtualDir.createUniqueTempDir();
                TarInputStream tis = null;
                OutputStream outStream = null;
                try {
                    if (isTgz(archiveFile.getName())) {
                        tis = new TarInputStream(
                                new GZIPInputStream(new BufferedInputStream(new FileInputStream(archiveFile))));
                    } else {
                        tis = new TarInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
                    }
                    TarEntry entry;

                    String longLink = null;
                    while ((entry = tis.getNextEntry()) != null) {
                        String entryName = entry.getName();
                        boolean entryIsLink = entry.getHeader().linkFlag == TarHeader.LF_LINK || entry.getHeader().linkFlag == LF_SPEC_LINK;
                        if (longLink != null && longLink.startsWith(entryName)) {
                            entryName = longLink;
                            longLink = null;
                        }
                        if (entry.isDirectory()) {
                            final File directory = new File(extractDir, entryName);
                            ensureDirectory(directory);
                            continue;
                        }

                        final String fileNameFromPath = getFilenameFromPath(entryName);
                        final int pathIndex = entryName.indexOf(fileNameFromPath);
                        String tarPath = null;
                        if (pathIndex > 0) {
                            tarPath = entryName.substring(0, pathIndex - 1);
                        }

                        File targetDir;
                        if (tarPath != null) {
                            targetDir = new File(extractDir, tarPath);
                        } else {
                            targetDir = extractDir;
                        }

                        ensureDirectory(targetDir);
                        final File targetFile = new File(targetDir, fileNameFromPath);
                        if (!entryIsLink && targetFile.isFile()) {
                            continue;
                        }

                        if (!entryIsLink && !targetFile.createNewFile()) {
                            throw new IOException("Unable to create file: " + targetFile.getAbsolutePath());
                        }

                        outStream = new BufferedOutputStream(new FileOutputStream(targetFile));
                        final byte data[] = new byte[1024 * 1024];
                        int count;
                        while ((count = tis.read(data)) != -1) {
                            outStream.write(data, 0, count);
                            //if the entry is a link, must be saved, since the name of the next entry depends on this
                            if (entryIsLink) {
                                longLink = (longLink == null ? "" : longLink) + new String(data, 0, count);
                            } else {
                                longLink = null;
                            }
                        }
                        //the last character is \u0000, so it must be removed
                        if (longLink != null) {
                            longLink = longLink.substring(0, longLink.length() - 1);
                        }
                        outStream.flush();
                        outStream.close();

                    }
                } finally {
                    if (tis != null) {
                        tis.close();
                    }
                    if (outStream != null) {
                        outStream.flush();
                        outStream.close();
                    }
                }
            }
        }

        private void ensureDirectory(File targetDir) throws IOException {
            if (!targetDir.isDirectory()) {
                if (!targetDir.mkdirs()) {
                    throw new IOException("unable to create directory: " + targetDir.getAbsolutePath());
                }
            }
        }

        public String[] listAll() {
            List<String> fileNames = new ArrayList<String>();
            TarInputStream tis = null;
            try {
                if (isTgz(archiveFile.getName())) {
                    tis = new TarInputStream(
                            new GZIPInputStream(new BufferedInputStream(new FileInputStream(archiveFile))));
                } else {
                    tis = new TarInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
                }
                TarEntry entry;

                String longLink = null;
                while ((entry = tis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    boolean entryIsLink = entry.getHeader().linkFlag == TarHeader.LF_LINK || entry.getHeader().linkFlag == LF_SPEC_LINK;
                    if (longLink != null && longLink.startsWith(entryName)) {
                        entryName = longLink;
                        longLink = null;
                    }
                    //if the entry is a link, must be saved, since the name of the next entry depends on this
                    if (entryIsLink) {
                        final byte data[] = new byte[1024 * 1024];
                        int count;
                        while ((count = tis.read(data)) != -1) {
                            longLink = (longLink == null ? "" : longLink) + new String(data, 0, count);
                        }
                    } else {
                        longLink = null;
                        fileNames.add(entryName);
                    }
                    //the last character is \u0000, so it must be removed
                    if (longLink != null) {
                        longLink = longLink.substring(0, longLink.length() - 1);
                    }
                }
            } catch (IOException e) {
                // cannot open/read tar, list will be empty
                fileNames = new ArrayList<String>();
            }
            return fileNames.toArray(new String[fileNames.size()]);
        }

        public void ensureUnpackedStarted() {
            if (!unpackStarted) {
                unpackStarted = true;
                executor.execute(unpackTask);
            }
        }
    }
}
