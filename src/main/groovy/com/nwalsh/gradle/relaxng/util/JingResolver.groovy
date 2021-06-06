package com.nwalsh.gradle.relaxng.util

import com.thaiopensource.resolver.BasicResolver
import com.thaiopensource.resolver.Identifier
import com.thaiopensource.resolver.Input
import com.thaiopensource.resolver.ResolverException
import com.thaiopensource.resolver.xml.ExternalDTDSubsetIdentifier
import com.thaiopensource.resolver.xml.ExternalEntityIdentifier
import com.thaiopensource.resolver.xml.ExternalIdentifier
import com.thaiopensource.resolver.catalog.ResolverIOException

import org.xmlresolver.Resolver
import org.xmlresolver.XMLResolverConfiguration
import org.xmlresolver.sources.ResolverInputSource;
import org.xmlresolver.sources.ResolverSAXSource;

class JingResolver extends com.thaiopensource.resolver.AbstractResolver {
  private final Resolver resolver

  JingResolver(String catalogs) {
    XMLResolverConfiguration config = null
    if (catalogs == null) {
      config = new XMLResolverConfiguration()
    } else {
      config = new XMLResolverConfiguration(catalogs)
    }
    resolver = new Resolver(config)
  }
  
  public void resolve(Identifier id, Input input) throws IOException, ResolverException {
    // This logic is largely copied from the com.thaiopensource classes
    if (input.isResolved()) {
      return
    }

    String absoluteUri = null
    try {
      absoluteUri = BasicResolver.resolveUri(id)
      if (id.getUriReference().equals(absoluteUri)) {
        absoluteUri = null
      }
    } catch (ResolverException ex) {
      // ignore
    }

    String resolved = null
    boolean isExternalIdentifier = (id instanceof ExternalIdentifier)

    try {
      if (absoluteUri != null) {
        resolved = (isExternalIdentifier
                    ? resolver.resolveEntity(null, absoluteUri)
                    : resolver.resolve(absoluteUri, (String) null))
      }

      if (resolved == null) {
        if (!isExternalIdentifier) {
          ResolverSAXSource rr = resolver.resolve(id.getUriReference(), null)
          if (rr != null) {
            resolved = rr.getSystemId()
          }
        } else {
          ResolverInputSource rr = null;
          if (id instanceof ExternalEntityIdentifier) {
            ExternalEntityIdentifier xid = (ExternalEntityIdentifier) id
            rr = resolver.resolveEntity(xid.getEntityName(), xid.getPublicId(),
                                        null, xid.getUriReference())
          } else if (id instanceof ExternalDTDSubsetIdentifier) {
            ExternalDTDSubsetIdentifier xid = (ExternalDTDSubsetIdentifier) id
            rr = resolver.getExternalSubset(xid.getDoctypeName(), xid.getUriReference())
          } else {
            ExternalIdentifier xid = (ExternalIdentifier) id
            rr = resolver.resolveEntity(xid.getPublicId(), xid.getUriReference())
          }
          if (rr != null) {
            resolved = rr.getSystemId()
          }
        }
      }
    } catch (ResolverIOException e) {
      throw e.getResolverException()
    }

    if (resolved != null)
      input.setUri(resolved)
  }

  public void open(Input input) throws IOException, ResolverException {
    if (!input.isUriDefinitive()) {
      return
    }

    URI uri
    try {
      uri = new URI(input.getUri())
    } catch (URISyntaxException e) {
      throw new ResolverException(e)
    }

    if (!uri.isAbsolute()) {
      throw new ResolverException("cannot open relative URI: " + uri)
    }

    URL url = new URL(uri.toASCIIString())
    // XXX should set the encoding properly
    // XXX if this is HTTP and we've been redirected, should do input.setURI with the new URI
    input.setByteStream(url.openStream())
  }
}

