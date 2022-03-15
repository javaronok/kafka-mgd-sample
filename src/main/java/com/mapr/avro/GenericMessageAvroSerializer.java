package com.mapr.avro;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GenericMessageAvroSerializer implements Serializer<List<GenericRecord>> {
  public static final String SCHEMA = "SCHEMA";

  private Schema schema = null;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    schema = (Schema) configs.get("SCHEMA");
  }

  @Override
  public byte[] serialize(String topic, List<GenericRecord> records) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GenericDatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);

    DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
    try {
      dataFileWriter.create(schema, out);
      for (GenericRecord record : records) {
        dataFileWriter.append(record);
      }
      dataFileWriter.flush();
      dataFileWriter.close();
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
