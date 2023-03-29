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
    //println("JingResolver attempting to resolve ${id.getUriReference()}")

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
      absoluteUri = null // irrelevant, but satisfies CodeNarc
    }

    boolean isExternalIdentifier = (id instanceof ExternalIdentifier)

    try {
      if (isExternalIdentifier) {
        ResolverInputSource resolved = null
        if (absoluteUri != null) {
          resolved = resolver.resolveEntity(null, absoluteUri)
        }
        if (resolved == null) {
          if (id instanceof ExternalEntityIdentifier) {
            ExternalEntityIdentifier xid = (ExternalEntityIdentifier) id
            resolved = resolver.resolveEntity(xid.getEntityName(), xid.getPublicId(),
                                              null, xid.getUriReference())
          } else if (id instanceof ExternalDTDSubsetIdentifier) {
            ExternalDTDSubsetIdentifier xid = (ExternalDTDSubsetIdentifier) id
            resolved = resolver.getExternalSubset(xid.getDoctypeName(), xid.getUriReference())
          } else {
            ExternalIdentifier xid = (ExternalIdentifier) id
            resolved = resolver.resolveEntity(xid.getPublicId(), xid.getUriReference())
          }
        }

        if (resolved != null) {
          input.setUri(resolved.getSystemId())
          input.setByteStream(resolved.getByteStream())
        }
      } else {
        ResolverSAXSource resolved = null
        if (absoluteUri != null) {
          resolved = ((ResolverSAXSource) resolver.resolve(absoluteUri, (String) null))
        }

        if (resolved == null) {
          resolved = resolver.resolve(id.getUriReference(), null)
        }

        if (resolved != null) {
          input.setUri(resolved.getInputSource().getSystemId())
          input.setByteStream(resolved.getInputSource().getByteStream())
        }
      }
    } catch (ResolverIOException e) {
      throw e.getResolverException()
    }
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

