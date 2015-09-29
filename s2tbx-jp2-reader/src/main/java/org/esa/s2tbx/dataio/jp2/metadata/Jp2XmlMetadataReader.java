package org.esa.s2tbx.dataio.jp2.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Reader for decoding JP2 XML blocks into Metadata elements.
 *
 * @author Cosmin Cara
 */
public class Jp2XmlMetadataReader {

    /* Start of contiguous codestream block (reversed) */
    private static final int[] JP2_JP2C = { 0x63, 0x32, 0x70, 0x6A };
    /* Start of any XML block (reversed) */
    private static final int[] JP2_XML = { 0x20, 0x6C, 0x6D, 0x78 };

    private ByteSequenceMatcher jp2cMatcher;
    private ByteSequenceMatcher xmlTagMatcher;

    private File jp2File;

    public Jp2XmlMetadataReader(File jp2File) {
        this.jp2File = jp2File;
        jp2cMatcher = new ByteSequenceMatcher(JP2_JP2C);
        xmlTagMatcher = new ByteSequenceMatcher(JP2_XML);
    }

    public Jp2XmlMetadata read() {
        Jp2XmlMetadata metadata = null;
        if (jp2File != null && jp2File.canRead()) {
            try (FileInputStream stream = new FileInputStream(jp2File)) {
                FileChannel channel = stream.getChannel();
                MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                int currentByte;
                while (mappedByteBuffer.hasRemaining() && !jp2cMatcher.matches((currentByte = mappedByteBuffer.get()))) {
                    if (xmlTagMatcher.matches(currentByte)) {
                        String xmlString = extractBlock(mappedByteBuffer);
                        if (metadata == null) {
                            metadata = Jp2XmlMetadata.create(Jp2XmlMetadata.class, xmlString);
                            metadata.setName("XML Metadata");
                        } else {
                            metadata.getRootElement().addElement(Jp2XmlMetadata.create(Jp2XmlMetadata.class, xmlString).getRootElement());
                        }
                    }
                }
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return metadata;
    }

    private String extractBlock(MappedByteBuffer buffer) throws IOException {
        StringBuilder builder = new StringBuilder();
        int current;
        while ((current = buffer.get()) != 0) {
            builder.append(Character.toString((char) current));
        }
        return builder.toString();
    }

    private class ByteSequenceMatcher {
        private int[] queue;
        private int[] sequence;

        ByteSequenceMatcher(int[] sequenceToMatch) {
            sequence = sequenceToMatch;
            queue = new int[sequenceToMatch.length];
        }

        public boolean matches(int unsignedByte) {
            insert(unsignedByte);
            return isMatch();
        }

        private void insert(int unsignedByte) {
            System.arraycopy(queue, 0, queue, 1, sequence.length - 1);
            queue[0] = unsignedByte;
        }

        private boolean isMatch() {
            boolean result = true;
            for (int i = 0; i < sequence.length; i++) {
                result = (queue[i] == sequence[i]);
                if (!result)
                    break;
            }
            return result;
        }
    }
}
