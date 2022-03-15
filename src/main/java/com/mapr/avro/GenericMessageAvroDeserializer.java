package com.mapr.avro;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericMessageAvroDeserializer implements Deserializer<List<GenericRecord>> {
  public static final String SCHEMA = "SCHEMA";

  private Schema schema = null;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    schema = (Schema) configs.get(SCHEMA);
  }

  @Override
  public List<GenericRecord> deserialize(String topic, byte[] data) {
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
    SeekableByteArrayInput arrayInput = new SeekableByteArrayInput(data);
    List<GenericRecord> records = new ArrayList<>();

    DataFileReader<GenericRecord> dataFileReader;
    try {
      dataFileReader = new DataFileReader<>(arrayInput, datumReader);
      while (dataFileReader.hasNext()) {
        GenericRecord record = dataFileReader.next();
        records.add(record);
      }
      return records;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
