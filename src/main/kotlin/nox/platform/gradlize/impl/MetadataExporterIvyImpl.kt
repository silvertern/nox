/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize.impl

import nox.platform.gradlize.Bundle
import nox.platform.gradlize.Dependency
import nox.platform.gradlize.MetadataExporter
import org.apache.commons.io.FileUtils
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


internal class MetadataExporterIvyImpl(private val bundle: Bundle, private val org: String, private val dependencies: Collection<Dependency>) : MetadataExporter {

	@Throws(IOException::class)
	override fun exportTo(targetDir: File) {
		targetDir.mkdirs()
		val file = File(targetDir, "%s-%s.xml".format(bundle.name, bundle.version))
		FileUtils.writeStringToFile(file, toString(), "UTF-8", false)
	}

	override fun toString(): String {
		try {
			val source = DOMSource(toDocument())
			StringWriter().use { writer ->
				val transformer = TransformerFactory.newInstance().newTransformer()
				transformer.setOutputProperty(OutputKeys.VERSION, "1.0")
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
				transformer.setOutputProperty(OutputKeys.INDENT, "yes")
				transformer.setOutputProperty(OutputKeys.STANDALONE, "yes")
				transformer.transform(source, StreamResult(writer))
				return writer.toString()
			}
		} catch (ex: ParserConfigurationException) {
			throw IllegalStateException(ex)
		} catch (ex: TransformerException) {
			throw IllegalStateException(ex)
		} catch (ex: IOException) {
			throw IllegalStateException(ex)
		}
	}

	private fun toDocument(): Document {
		val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

		val ivyModElm = doc.createElement("ivy-module")
		doc.appendChild(ivyModElm)
		ivyModElm.setAttribute("version", "2.0")

		val infoElm = doc.createElement("info")
		ivyModElm.appendChild(infoElm)
		infoElm.setAttribute("organisation", org)
		infoElm.setAttribute("module", bundle.name)
		infoElm.setAttribute("revision", bundle.version.toString())
		infoElm.setAttribute("status", "release")
		// infoElm.setAttribute("default", "true");

		val confElm = doc.createElement("configurations")
		ivyModElm.appendChild(confElm)
		confElm.setAttribute("defaultconfmapping", "default")
		var elm = doc.createElement("conf")
		confElm.appendChild(elm)
		elm.setAttribute("name", "compile")
		elm = doc.createElement("conf")
		confElm.appendChild(elm)
		elm.setAttribute("name", "default")
		elm.setAttribute("extends", "compile")

		val depsElm = doc.createElement("dependencies")
		ivyModElm.appendChild(depsElm)
		for (dep in dependencies) {
			val depElm = doc.createElement("dependency")
			depsElm.appendChild(depElm)
			depElm.setAttribute("org", org)
			depElm.setAttribute("name", dep.name)
			depElm.setAttribute("rev", dep.version.toString())
			depElm.setAttribute("conf", "compile->default")
		}
		return doc
	}
}
