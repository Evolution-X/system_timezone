/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.libcore.timezone.tzlookup;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * A class that knows about the structure of the tzlookup.xml file.
 */
final class TzLookupFile {
    private static final String TIMEZONES_ELEMENT = "timezones";
    private static final String IANA_VERSION_ATTRIBUTE = "ianaversion";
    private static final String COUNTRY_ZONES_ELEMENT = "countryzones";
    private static final String COUNTRY_ELEMENT = "country";
    private static final String COUNTRY_CODE_ATTRIBUTE = "code";
    private static final String DEFAULT_ATTRIBUTE = "default";
    private static final String EVER_USES_UTC_ATTRIBUTE = "everutc";
    private static final String ID_ELEMENT = "id";

    static void write(TimeZones timeZones, String outputFile)
            throws XMLStreamException, IOException {
        /*
         * The required XML structure is:
         * <timezones ianaversion="2017b">
         *   <countryzones>
         *     <country code="us" default="America/New_York" everutc="n">
         *       <!-- -5:00 -->
         *       <id>America/New_York"</id>
         *       ...
         *       <!-- -8:00 -->
         *       <id>America/Los_Angeles</id>
         *       ...
         *     </country>
         *     <country code="gb" default="Europe/London" everutc="y">
         *       <!-- 0:00 -->
         *       <id>Europe/London</id>
         *     </country>
         *   </countryzones>
         * </timezones>
         */

        StringWriter writer = new StringWriter();
        writeRaw(timeZones, writer);
        String rawXml = writer.getBuffer().toString();

        TransformerFactory factory = TransformerFactory.newInstance();
        try (Writer fileWriter = new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {

            // Transform the XML with the identity transform but with indenting
            // so it's more human-readable.
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");
            transformer.transform(
                    new StreamSource(new StringReader(rawXml)), new StreamResult(fileWriter));
        } catch (TransformerException e) {
            throw new XMLStreamException(e);
        }
    }

    private static void writeRaw(TimeZones timeZones, Writer fileWriter)
            throws XMLStreamException {
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(fileWriter);
        xmlWriter.writeStartDocument();
        xmlWriter.writeComment("\n\n **** Autogenerated file - DO NOT EDIT ****\n\n");
        TimeZones.writeXml(timeZones, xmlWriter);
        xmlWriter.writeEndDocument();
    }

    static class TimeZones {

        private final String ianaVersion;
        private CountryZones countryZones;

        TimeZones(String ianaVersion) {
            this.ianaVersion = ianaVersion;
        }

        void setCountryZones(CountryZones countryZones) {
            this.countryZones = countryZones;
        }

        static void writeXml(TimeZones timeZones, XMLStreamWriter writer)
                throws XMLStreamException {
            writer.writeStartElement(TIMEZONES_ELEMENT);
            writer.writeAttribute(IANA_VERSION_ATTRIBUTE, timeZones.ianaVersion);
            CountryZones.writeXml(timeZones.countryZones, writer);
            writer.writeEndElement();
        }
    }

    static class CountryZones {

        private final List<Country> countries = new ArrayList<>();

        CountryZones() {
        }

        static void writeXml(CountryZones countryZones, XMLStreamWriter writer)
                throws XMLStreamException {
            writer.writeStartElement(COUNTRY_ZONES_ELEMENT);
            for (Country country : countryZones.countries) {
                Country.writeXml(country, writer);
            }
            writer.writeEndElement();
        }

        void addCountry(Country country) {
            countries.add(country);
        }
    }

    static class Country {

        private final String isoCode;
        private final String defaultTimeZoneId;
        private final boolean everUsesUtc;
        private final List<TimeZoneIdentifier> timeZoneIds = new ArrayList<>();

        Country(String isoCode, String defaultTimeZoneId, boolean everUsesUtc) {
            this.defaultTimeZoneId = defaultTimeZoneId;
            this.isoCode = isoCode;
            this.everUsesUtc = everUsesUtc;
        }

        void addTimeZoneIdentifier(TimeZoneIdentifier timeZoneId) {
            timeZoneIds.add(timeZoneId);
        }

        static void writeXml(Country country, XMLStreamWriter writer)
                throws XMLStreamException {
            writer.writeStartElement(COUNTRY_ELEMENT);
            writer.writeAttribute(COUNTRY_CODE_ATTRIBUTE, country.isoCode);
            writer.writeAttribute(DEFAULT_ATTRIBUTE, country.defaultTimeZoneId);
            writer.writeAttribute(EVER_USES_UTC_ATTRIBUTE, country.everUsesUtc ? "y" : "n");
            for (TimeZoneIdentifier timeZoneId : country.timeZoneIds) {
                TimeZoneIdentifier.writeXml(timeZoneId, writer);
            }
            writer.writeEndElement();
        }
    }

    static class TimeZoneIdentifier {

        private final String olsonId;

        TimeZoneIdentifier(String olsonId) {
            this.olsonId = olsonId;
        }

        static void writeXml(TimeZoneIdentifier timeZoneId, XMLStreamWriter writer)
                throws XMLStreamException {
            writer.writeStartElement(ID_ELEMENT);
            writer.writeCharacters(timeZoneId.olsonId);
            writer.writeEndElement();
        }
    }
}