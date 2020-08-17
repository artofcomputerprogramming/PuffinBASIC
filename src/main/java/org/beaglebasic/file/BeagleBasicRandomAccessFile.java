package org.beaglebasic.file;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntList;
import org.beaglebasic.domain.BeagleBasicSymbolTable;
import org.beaglebasic.error.BeagleBasicInternalError;
import org.beaglebasic.error.BeagleBasicRuntimeError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.beaglebasic.domain.STObjects.BeagleBasicDataType.STRING;
import static org.beaglebasic.error.BeagleBasicRuntimeError.ErrorCode.ILLEGAL_FILE_ACCESS;
import static org.beaglebasic.error.BeagleBasicRuntimeError.ErrorCode.IO_ERROR;

public class BeagleBasicRandomAccessFile implements BeagleBasicFile {

    private final String filename;
    private final FileAccessMode accessMode;
    private final RandomAccessFile file;
    private final int recordLength;
    private final byte[] recordBuffer;
    private IntList recordParts;
    private long currentFilePosBytes;
    private int lastGetRecordNumber;
    private int lastPutRecordNumber;
    private FileState fileState;

    public BeagleBasicRandomAccessFile(
            @NotNull String filename,
            @NotNull FileAccessMode accessMode,
            int recordLen)
    {
        Preconditions.checkNotNull(filename);
        Preconditions.checkArgument(recordLen > 0);
        Preconditions.checkNotNull(accessMode);

        this.filename = filename;
        this.accessMode = accessMode;
        this.recordLength = recordLen;
        this.recordBuffer = new byte[recordLength];
        this.lastPutRecordNumber = this.lastGetRecordNumber = -1;
        this.currentFilePosBytes = 0;

        try {
            this.file = new RandomAccessFile(filename, accessMode.mode);
        } catch (FileNotFoundException e) {
            throw new BeagleBasicRuntimeError(
                    IO_ERROR,
                    "Failed to open file '" + filename + "' for writing, error: "
                            + e.getMessage()
            );
        }

        this.fileState = FileState.OPEN;
    }

    @Override
    public void setFieldParams(
            BeagleBasicSymbolTable symbolTable,
            IntList recordParts)
    {
        Preconditions.checkNotNull(symbolTable);
        Preconditions.checkNotNull(recordParts);

        int totalComputedLength = 0;
        for (var recordPart : recordParts) {
            var entry = symbolTable.get(recordPart).getValue();
            var dataType = entry.getDataType();
            if (dataType != STRING) {
                throw new BeagleBasicInternalError(
                        "Expected String recordPart but found: " + dataType
                );
            }
            totalComputedLength += entry.getFieldLength();
        }
        if (totalComputedLength != recordLength) {
            throw new BeagleBasicInternalError(
                    "Sum of capacity of recordParts (=" + totalComputedLength
                            + ") don't match recordLength (=" + recordLength + ")"
            );
        }
        this.recordParts = recordParts;
    }

    @Override
    public int getCurrentRecordNumber() {
        assertOpen();
        return (int) (currentFilePosBytes / recordLength);
    }

    @Override
    public long getFileSizeInBytes() {
        assertOpen();
        try {
            return file.length();
        } catch (IOException e) {
            throw new BeagleBasicRuntimeError(
                    IO_ERROR,
                    "Failed to get length of the file '" + filename
                            + "', error: " + e.getMessage()
            );
        }
    }

    @Override
    public boolean eof() {
        return currentFilePosBytes >= getFileSizeInBytes();
    }

    @Override
    public void put(@Nullable Integer recordNumber, BeagleBasicSymbolTable symbolTable) {
        assertOpen();
        if (accessMode == FileAccessMode.READ_ONLY) {
            throw new BeagleBasicRuntimeError(
                    ILLEGAL_FILE_ACCESS,
                    "File " + filename + " is open for read-only"
            );
        }

        if (recordNumber == null) {
            recordNumber = lastPutRecordNumber + 1;
        }
        seekToRecord(recordNumber);
        this.lastPutRecordNumber = recordNumber;

        // Create a new buffer and fill with spaces.
        var bb = clearAndGetRecordBuffer();

        for (int i = 0; i < recordParts.size(); i++) {
            var entry = symbolTable.get(recordParts.getInt(i)).getValue();
            var value = entry.getString();
            var valueLength = value.length();
            var fieldLength = entry.getFieldLength();

            // Put first fieldLength bytes only
            if (fieldLength < valueLength) {
                value = value.substring(0, fieldLength);
            }
            bb.put(value.getBytes());

            // If fieldLength > valueLength, skip fieldLength - valueLength
            if (fieldLength > valueLength) {
                bb.position(bb.position() + fieldLength - valueLength);
            }

        }

        // Write the record buffer to file
        try {
            file.write(recordBuffer);
        } catch (IOException e) {
            throw new BeagleBasicRuntimeError(
                    IO_ERROR,
                    "Failed to write to file '" + filename + "', error: " + e.getMessage()
            );
        }

        updateCurrentBytePos();
    }

    @Override
    public void get(@Nullable Integer recordNumber, BeagleBasicSymbolTable symbolTable) {
        assertOpen();
        if (accessMode == FileAccessMode.WRITE_ONLY) {
            throw new BeagleBasicRuntimeError(
                    ILLEGAL_FILE_ACCESS,
                    "File " + filename + " is open for write-only"
            );
        }

        if (recordNumber == null) {
            recordNumber = lastGetRecordNumber + 1;
        }
        seekToRecord(recordNumber);
        this.lastGetRecordNumber = recordNumber;

        // Seek to record number and read the record into record buffer
        try {
            file.readFully(recordBuffer);
        } catch (IOException e) {
            throw new BeagleBasicRuntimeError(
                    IO_ERROR,
                    "Failed to read from file '" + filename + "', error: " + e.getMessage()
            );
        }

        var bb = ByteBuffer.wrap(recordBuffer);
        for (int i = 0; i < recordParts.size(); i++) {
            var entry = symbolTable.get(recordParts.getInt(i)).getValue();
            var fieldLength = entry.getFieldLength();
            var strBytes = new byte[fieldLength];
            bb.get(strBytes);
            entry.setString(new String(strBytes));
        }

        updateCurrentBytePos();
    }

    private ByteBuffer clearAndGetRecordBuffer() {
        Arrays.fill(recordBuffer, 0, recordBuffer.length, (byte) ' ');
        return ByteBuffer.wrap(recordBuffer);
    }

    private void updateCurrentBytePos() {
        currentFilePosBytes += recordLength;
    }

    private long getRecordBytePos(long recordNumber) {
        return recordNumber * recordLength;
    }

    private void seekToRecord(int recordNumber) {
        // Seek only when record number is not sequential
        try {
            long destPosBytes = getRecordBytePos(recordNumber);
            if (destPosBytes != currentFilePosBytes) {
                file.seek(destPosBytes);
                currentFilePosBytes = destPosBytes;
            }
        } catch (IOException e) {
            throw new BeagleBasicRuntimeError(
                    IO_ERROR,
                    "Failed to read from file '" + filename + "', error: " + e.getMessage()
            );
        }
    }

    private void assertOpen() {
        if (!isOpen()) {
            throw new BeagleBasicRuntimeError(
                    ILLEGAL_FILE_ACCESS,
                    "File " + filename + " is not open!"
            );
        }
    }

    @Override
    public boolean isOpen() {
        return fileState == FileState.OPEN;
    }

    @Override
    public void close() {
        assertOpen();
        try {
            this.file.close();
        } catch (Exception e) {
            throw new BeagleBasicRuntimeError(
                    IO_ERROR,
                    "Failed to close file '" + filename + "', error: " + e.getMessage()
            );
        }
        this.fileState = FileState.CLOSED;
    }

    @Override
    public byte[] readBytes(int n) {
        throw new BeagleBasicRuntimeError(
                ILLEGAL_FILE_ACCESS,
                "Can't read single bytes from RandomAccessFile!"
        );
    }

    @Override
    public void print(String s) {
        throw new BeagleBasicRuntimeError(
                ILLEGAL_FILE_ACCESS,
                "Not implemented for RandomAccessFile!"
        );
    }

    @Override
    public String readLine() {
        throw new BeagleBasicRuntimeError(
                ILLEGAL_FILE_ACCESS,
                "Not implemented for RandomAccessFile!"
        );
    }

    @Override
    public void writeByte(byte b) {
        throw new BeagleBasicRuntimeError(
                ILLEGAL_FILE_ACCESS,
                "Not implemented for RandomAccessFile!"
        );
    }
}