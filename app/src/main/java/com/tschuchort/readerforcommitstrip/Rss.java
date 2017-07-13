package com.tschuchort.readerforcommitstrip;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.List;

/*
 * this class has to be in Java because SimpleXML annotations with kotlin are weird
 */

@SuppressWarnings("WeakerAccess")
@Root
public class Rss {
    @Attribute
    public String version;

    @Element
    public Channel channel;

    @Override
    public String toString() {
        return "RSS{" +
                "version='" + version + '\'' +
                ", channel=" + channel +
                '}';
    }

    @NamespaceList({@Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom")})
    @Root(strict = false)
    public static class Channel {
        // Tricky part in Simple XML because the link is named twice
        @ElementList(entry = "link", inline = true, required = false)
        public List<Link> links;

        @ElementList(name = "item", inline = true)
        public List<Item> itemList;

        @Element
        String title;

        @Element
        String language;

        @Element(name = "ttl", required = false)
        int ttl;

        @Element(name = "pubDate", required = false)
        String pubDate;

        @Override
        public String toString() {
            return "Channel{" +
                    "links=" + links +
                    ", itemList=" + itemList +
                    ", title='" + title + '\'' +
                    ", language='" + language + '\'' +
                    ", ttl=" + ttl +
                    ", pubDate='" + pubDate + '\'' +
                    '}';
        }

        public static class Link {
            @Attribute(required = false)
            public String href;

            @Attribute(required = false)
            public String rel;

            @Attribute(name = "type", required = false)
            public String type;

            @Text(required = false)
            public String link;
        }

        @Root(name = "item", strict = false)
        public static class Item {
            @Element(name = "title")
            String title;

            @Element(name = "link")
            String link;

            @Element(name = "description")
            String description;

            @Element(name = "author", required = false)
            String author;

            @Element(name = "category", required = false)
            String category;

            @Element(name = "comments", required = false)
            String comments;

            @Element(name = "enclosure", required = false)
            String enclosure;

            @Element(name = "guid", required = false)
            String guid;

            @Element(name = "pubDate", required = false)
            String pubDate;

            @Element(name = "source", required = false)
            String source;

            @Override
            public String toString() {
                return "Item{" +
                        "title='" + title + '\'' +
                        ", link='" + link + '\'' +
                        ", description='" + description + '\'' +
                        ", author='" + author + '\'' +
                        ", category='" + category + '\'' +
                        ", comments='" + comments + '\'' +
                        ", enclosure='" + enclosure + '\'' +
                        ", guid='" + guid + '\'' +
                        ", pubDate='" + pubDate + '\'' +
                        ", source='" + source + '\'' +
                        '}';
            }
        }
    }
}