package com.norconex.collector.http.handler.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.collector.http.handler.IURLExtractor;
import com.norconex.collector.http.util.QuietConfigurationLoader;
import com.norconex.importer.ContentType;

/**
 * Default implementation of {@link IURLExtractor}.  
 * <p>
 * XML configuration usage (not required since default):
 * </p>
 * <pre>
 *  &lt;urlExtractor class="com.norconex.collector.http.handler.DefaultURLExtractor"&gt;
 *      &lt;maxURLLength&gt;
 *          (Optional maximum URL length.  Longer URLs won't be extracted.
 *           Default is 2048.)
 *      &lt;/maxURLLength&gt;
 *  &lt;/urlExtractor&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class DefaultURLExtractor implements IURLExtractor, IXMLConfigurable {

    private static final long serialVersionUID = 4130729871145622411L;
    private static final Logger LOG = LogManager.getLogger(
            DefaultURLExtractor.class);

    public static final int DEFAULT_MAX_URL_LENGTH = 2048;
    
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(href|src)(\\s*=\\s*)([\"']{0,1})(.+?)([\"'>])",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private int maxURLLength = DEFAULT_MAX_URL_LENGTH;
    
    @Override
    public Set<String> extractURLs(
            Reader document, String documentUrl, ContentType contentType)
            throws IOException {
        // Do not extract if non-HTML
        if (!contentType.equals(ContentType.HTML)) {
            return null;
        }
        
        // Extract URLs from HTML
        
        String protocol = documentUrl.replaceFirst("(.*?://)(.*)", "$1");
        String path = documentUrl.replaceFirst("(.*?://)(.*)", "$2");
        
        // Relative Base: truncate to last / before a ? or #
        String relativeBase = path.replaceFirst(
                "(.*?)([\\?\\#])(.*)", "$1");
        relativeBase = protocol +  path.replaceFirst("(.*/)(.*)", "$1");

        // Absolute Base: truncate to first / (if present) after protocol
        String absoluteBase = protocol + path.replaceFirst("(.*?)(/.*)", "$1");
        
        //TODO HOW TO HANDLE <BASE>????? Is it handled by Tika???

        if (LOG.isDebugEnabled()) {
            LOG.debug("DOCUMENT URL ----> " + documentUrl);
            LOG.debug("  BASE RELATIVE -> " + relativeBase);
            LOG.debug("  BASE ABSOLUTE -> " + absoluteBase);
        }

        Set<String> urls = new HashSet<String>();
        
        BufferedReader reader = new BufferedReader(document);
        String line;
        while ((line = reader.readLine()) != null)   {
            Matcher matcher = URL_PATTERN.matcher(line);
            while (matcher.find()) {
                String url = matcher.group(4);
                if (url.startsWith("mailto:")) {
//                if (url.startsWith("mailto:") 
//                        || url.startsWith("data:image")) {
                    continue;
                }
                if (url.startsWith("/")) {
                    url = absoluteBase + url;
                } else if (!url.contains("://")) {
                    url = relativeBase + url;
                }
                //TODO have configurable whether to strip anchors.
                url = StringUtils.substringBefore(url, "#");
                if (url.length() > maxURLLength) {
                    LOG.warn("URL length (" + url.length() + ") exeeding "
                           + "maximum length allowed (" + maxURLLength
                           + ") to be extracted. URL (showing first 200 "
                           + "chars): " + StringUtils.substring(url, 0, 200));
                } else {
                    urls.add(url);
                }
            }
        }
        return urls;
     }

    public int getMaxURLLength() {
        return maxURLLength;
    }
    public void setMaxURLLength(int maxURLLength) {
        this.maxURLLength = maxURLLength;
    }

    @Override
    public void loadFromXML(Reader in) {
        XMLConfiguration xml = QuietConfigurationLoader.load(in);
        setMaxURLLength(xml.getInt("maxURLLength", DEFAULT_MAX_URL_LENGTH));
    }
    @Override
    public void saveToXML(Writer out) {
        // TODO Implement me.
        System.err.println("saveToXML not implemented");
    }
}