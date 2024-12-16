import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/*    

    this code to Differentiate between audio Wav and Ogg format

*/

@SuppressWarnings({ "resource", "CallToPrintStackTrace" })
public class Main {
    public static void main(String[] args) {
        JFrame jF = new JFrame();
        jF.setAlwaysOnTop(true);
        JOptionPane.showMessageDialog(jF, "Select a WAV or OGG file");

        JFileChooser jf = new JFileChooser();
        jf.showOpenDialog(jF);

        FileHeader fileHeader = new FileHeader(jf.getSelectedFile().getAbsolutePath());
        fileHeader.setFileHeader();

        JOptionPane.showMessageDialog(jF, fileHeader.getHeader());
        jF.dispose();

        try {
            FileOutputStream fos = new FileOutputStream(jf.getSelectedFile().getPath() + ".FileHeader.txt");
            fos.write(fileHeader.getHeader().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

@SuppressWarnings({ "CallToPrintStackTrace", "ResultOfMethodCallIgnored" })
class FileHeader {
    private final File file;
    private final Map<String, byte[]> fileHeader = new LinkedHashMap<>();

    public FileHeader(String path) {
        file = new File(path);
        try (FileInputStream fis = new FileInputStream(file)) {
            fileHeader.put("File Signature", readBigEndian(fis));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setFileHeader() {
        String fileSignature = new String(fileHeader.get("File Signature"));
        switch (fileSignature) {
            case "RIFF":
                readWav(file);
                break;
            case "OggS":
                readOgg(file);
                break;
            default:
                System.err.println("Error: Invalid file format");
                JOptionPane.showMessageDialog(null, "Error: Invalid file format");
                System.exit(1);
                break;
        }
    }

    private void readOgg(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(4);
            fileHeader.put("Version", readNBytes(fis, 1));
            fileHeader.put("Header Type", readNBytes(fis, 1));
            fileHeader.put("Granule Position", readLongLittleEndian(fis));
            fileHeader.put("Bitstream Serial Number", readLittleEndian(fis));
            fileHeader.put("Page Sequence Number", readLittleEndian(fis));
            fileHeader.put("Checksum", readLittleEndian(fis));
            fileHeader.put("Page Segments", readNBytes(fis, 1));
            fileHeader.put("Segment Table", readNBytes(fis, fileHeader.get("Page Segments")[0]));
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readWav(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(4);
            fileHeader.put("File Size", readLittleEndian(fis));
            fileHeader.put("Format", readBigEndian(fis));
            fileHeader.put("Subchunk1 ID", readBigEndian(fis));
            fileHeader.put("Subchunk1 Size", readLittleEndian(fis));
            fileHeader.put("Audio Format", readShortLittleEndian(fis));
            fileHeader.put("Number of Channels", readShortLittleEndian(fis));
            fileHeader.put("Sample Rate", readLittleEndian(fis));
            fileHeader.put("Byte Rate", readLittleEndian(fis));
            fileHeader.put("Block Align", readShortLittleEndian(fis));
            fileHeader.put("Bits Per Sample", readShortLittleEndian(fis));
            fileHeader.put("Subchunk2 ID", readBigEndian(fis));
            fileHeader.put("Subchunk2 Size", readLittleEndian(fis));
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] readNBytes(FileInputStream fis, int byteCount) throws IOException {
        byte[] buffer = new byte[byteCount];
        fis.read(buffer, 0, byteCount);
        return buffer;
    }

    private byte[] readLongLittleEndian(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[8];
        fis.read(buffer);
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        return wrappedBuffer.array();
    }

    private byte[] readBigEndian(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[4];
        fis.read(buffer);
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN);
        return wrappedBuffer.array();
    }

    private byte[] readLittleEndian(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[4];
        fis.read(buffer);
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        return wrappedBuffer.array();
    }

    private byte[] readShortLittleEndian(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[2];
        fis.read(buffer);
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        return wrappedBuffer.array();
    }

    public String getHeader() {
        switch (new String(fileHeader.get("File Signature"))) {
            case "RIFF":
                return getWavHeader();
            case "OggS":
                return getOggHeader();
        }
        return null;
    }

    private int[] convertToUnsignedInt(byte[] bytes) {
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = Byte.toUnsignedInt(bytes[i]);
        }
        return result;
    }

    private boolean isWavCorrupted() {
        try {
            int[] fileSize = convertToUnsignedInt(fileHeader.get("File Size"));
            int[] subchunk2Size = convertToUnsignedInt(fileHeader.get("Subchunk2 Size"));

            if (fileSize.length == 4 && subchunk2Size.length == 4) {
                return fileSize[0] == 0 || subchunk2Size[0] == 0;
            } else {
                System.err.println("Error: Invalid header format for WAV file");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean isOggCorrupted() {
        try {
            int[] pageSegments = convertToUnsignedInt(fileHeader.get("Page Segments"));

            return pageSegments.length != 1 || pageSegments[0] < 0 || pageSegments[0] > 255;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public String getWavHeader() {
        boolean isCorrupted = isWavCorrupted();
        return "This is a WAVE file\n" +
                "WAV Header" + "\n" +
                "File Size=" + formatFileSize(convertToUnsignedInt(fileHeader.get("File Size"))) + "\n" +
                "Format=" + new String(fileHeader.get("Format")) + "\n" +
                "Subchunk1 ID=" + new String(fileHeader.get("Subchunk1 ID")) + "\n" +
                "Subchunk1 Size=" + formatFileSize(convertToUnsignedInt(fileHeader.get("Subchunk1 Size"))) + "\n" +
                "Audio Format=" + formatAudioFormat(convertToUnsignedInt(fileHeader.get("Audio Format"))) + "\n" +
                "Number of Channels=" + formatChannels(convertToUnsignedInt(fileHeader.get("Number of Channels")))
                + "\n" +
                "Sample Rate=" + formatSampleRate(convertToUnsignedInt(fileHeader.get("Sample Rate"))) + "\n" +
                "Byte Rate=" + formatByteRate(convertToUnsignedInt(fileHeader.get("Byte Rate"))) + "\n" +
                "Block Align=" + formatBlockAlign(convertToUnsignedInt(fileHeader.get("Block Align"))) + "\n" +
                "Bits Per Sample=" + formatBitsPerSample(convertToUnsignedInt(fileHeader.get("Bits Per Sample"))) + "\n"
                +
                "Subchunk2 ID=" + new String(fileHeader.get("Subchunk2 ID")) + "\n" +
                "Subchunk2 Size=" + formatFileSize(convertToUnsignedInt(fileHeader.get("Subchunk2 Size"))) + "\n" +
                "Corrupted=" + isCorrupted;
    }

    public String getOggHeader() {
        boolean isCorrupted = isOggCorrupted();
        return "This is an OGG file\n" +
                "File Signature: " + new String(fileHeader.get("File Signature")) + "\n" +
                "Version: " + formatVersion(convertToUnsignedInt(fileHeader.get("Version"))) + "\n" +
                "Header Type: " + formatHeaderType(convertToUnsignedInt(fileHeader.get("Header Type"))) + "\n" +
                "Granule Position: " + formatGranulePosition(convertToUnsignedInt(fileHeader.get("Granule Position")))
                + "\n" +
                "Bitstream Serial Number: "
                + formatBitstreamSerialNumber(convertToUnsignedInt(fileHeader.get("Bitstream Serial Number"))) + "\n" +
                "Page Sequence Number: "
                + formatPageSequenceNumber(convertToUnsignedInt(fileHeader.get("Page Sequence Number"))) + "\n" +
                "Checksum: " + formatChecksum(convertToUnsignedInt(fileHeader.get("Checksum"))) + "\n" +
                "Page Segments: " + formatPageSegments(convertToUnsignedInt(fileHeader.get("Page Segments"))) + "\n" +
                "Segment Table: " + formatSegmentTable(fileHeader.get("Segment Table")) + "\n" +
                "Corrupted: " + isCorrupted;
    }

    private String formatVersion(int[] version) {
        return "0x" + Integer.toHexString(version[0]);
    }

    private String formatHeaderType(int[] headerType) {
        return "0x" + Integer.toHexString(headerType[0]);
    }

    private String formatGranulePosition(int[] granulePosition) {
        return String.valueOf(toLittleEndianLong(granulePosition));
    }

    private String formatBitstreamSerialNumber(int[] bitstreamSerialNumber) {
        return String.valueOf(toLittleEndianLong(bitstreamSerialNumber));
    }

    private String formatPageSequenceNumber(int[] pageSequenceNumber) {
        return String.valueOf(toLittleEndianLong(pageSequenceNumber));
    }

    private String formatChecksum(int[] checksum) {
        return "0x" + Integer.toHexString(checksum[0]);
    }

    private String formatPageSegments(int[] pageSegments) {
        return String.valueOf(pageSegments[0]);
    }

    private String formatSegmentTable(byte[] segmentTable) {
        // Convert byte values to a string representation
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : segmentTable) {
            stringBuilder.append(String.format("0x%02X ", b));
        }
        return stringBuilder.toString();
    }

    private String formatFileSize(int[] fileSize) {
        long sizeInBytes = toLittleEndianLong(fileSize);
        return formatFileSize(sizeInBytes);
    }

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " bytes";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));
        }
    }

    private String formatAudioFormat(int[] audioFormat) {
        int formatCode = audioFormat[0];
        switch (formatCode) {
            case 1:
                return "PCM (Linear Quantization)";
            case 3:
                return "IEEE Float";
            default:
                return "Unknown";
        }
    }

    private String formatChannels(int[] numChannels) {
        int channels = numChannels[0];
        return channels + " channel" + (channels > 1 ? "s" : "");
    }

    private String formatSampleRate(int[] sampleRate) {
        return sampleRate[0] + " Hz";
    }

    private String formatByteRate(int[] byteRate) {
        long rate = toLittleEndianLong(byteRate);
        return formatFileSize(rate) + "/s";
    }

    private String formatBlockAlign(int[] blockAlign) {
        return blockAlign[0] + " bytes";
    }

    private String formatBitsPerSample(int[] bitsPerSample) {
        return bitsPerSample[0] + " bits";
    }

    private long toLittleEndianLong(int[] bytes) {
        long result = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }
}
