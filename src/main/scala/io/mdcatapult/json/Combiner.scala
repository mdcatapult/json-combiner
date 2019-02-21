package io.mdcatapult.json

import ch.qos.logback.classic.{Level, Logger}
import com.typesafe.scalalogging.LazyLogging
import scopt.OParser
import java.io.{File, PrintWriter}

import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.io.Source
import play.api.libs.json._
import play.api.libs.json.implicits.JsonXmlImplicits._

import scala.util.{Failure, Success, Try}
import scala.xml._

/**
  * Class for handling cli arguments
  * @param in the input directory to scan for files files
  * @param out the output directory save files to
  * @param size the size of file to generate in bytes (default 16Mb-16kb = 16760832)
  * @param prefix prefix to give to the generated files
  * @param recursive folder will be scanned recursively
  * @param verbose show information while processing
  * @param debug show debug
  * @param format format of file that should be parsed
  * @param element path to element to use as the root of the document
  */
sealed case class Config(
                          in: File = new File("."),
                          out: File = new File("."),
                          size: Int = 16760832, // 16Mb - 16Kb
                          prefix: String = "",
                          recursive: Boolean = false,
                          verbose: Boolean = false,
                          debug: Boolean = false,
                          format: String = "json",
                          element: List[String] = List[String]())


/**
  * Application Object for the JSON Combiner CLI
  */
object Combiner extends App with LazyLogging {
  val builder = OParser.builder[Config]
  /**
    * `Scopt` input parser to define inputs and usage
    */
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("json-combiner"),
      head("json-combiner", "1.x"),
      opt[String]('i', "in")
        .action((x, c) => c.copy(in = new File(x)))
        .text("the input directory to scan for files files (required)")
        .required(),
      opt[String]('o', "out")
        .action((x, c) => c.copy(out = new File(x)))
        .text("the output directory save files to (required)")
        .required(),
      opt[Int]('s', "size")
        .action((x, c) => c.copy(size = x))
        .text("the size of file to generate in bytes (default 16Mb-16kb = 16760832) "),
      opt[String]('f', "format")
        .action((x, c) => c.copy(format = x))
        .text("format of file that should be parsed (default json)"),
      opt[String]('p', "prefix")
        .action((x, c) => c.copy(prefix = x))
        .text("prefix to give to the generated files (default none)"),
      opt[String]('e', "element")
        .action((x, c) => c.copy(element = x.split('.').toList))
        .text("path to element to use as the root of the document using dot notation (default none)"),
      opt[Unit]('r', "recursive")
        .action((_, c) => c.copy(recursive = true))
        .text("folder will be scanned recursively"),
      opt[Unit]('v', "verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("show information while processing"),
      opt[Unit]("debug")
        .action((_, c) => c.copy(debug = true))
        .text("show debug")

    )
  }

  /**
    * *Side Effect* validate config and short cut to exit where required
    * @param config cli config class
    * @return
    */
  def validateConfig(config: Config) = {
    if (!config.in.exists()) {
      logger.error("Input directory does not exist!")
      sys.exit(1)
    }

    if (!config.in.isDirectory) {
      logger.error("Input directory must be a valid directory!")
      sys.exit(1)
    }

    if (!config.out.exists()) {
      config.out.mkdirs()
    }
  }

  /**
    * Write output file as side effect
    * @param config cli config class
    * @param files contents that should be written
    * @param suffix numeric index to be added to the end of the filename
    */
  def writeFile(config: Config, files: List[JsValue], suffix: Int): Unit = {
    var filename = f"${config.in.getName}-$suffix.json"
    if (config.prefix.nonEmpty) {
      filename = s"${config.prefix}-$filename"
    }
    val target = f"${config.out.toString}/$filename"
    val content: String = Json.toJson(files).toString()
    logger.info(f"WRITING: ${files.length} json documents to $target")

    val targetFile = new File(target)

    val writer = new PrintWriter(targetFile)
    writer.write(content)
    writer.close()

    logger.debug(f"BYTES: ${targetFile.length()} written to $target")
  }

  /**
    * traverse filesystem and identify files to convert & combine
    * // @tailrec
    * @param dir directory to scan
    * @param recursive flag to scan recursively
    * @param format string to identify the file extension/format
    * @return
    */
  def getFiles(dir: File, recursive: Boolean = false, format: String = "json"): Array[File] = {
    val files = dir.listFiles.filter(f ⇒ f.isFile && f.getName.endsWith(f".$format"))
    if (files.length == 0) {
      logger.info(f"SCANNING: found ${files.length} $format files in ${dir.toString}")
    } else {
      logger.debug(f"SCANNING: found ${files.length} $format files in ${dir.toString}")
    }
    if (recursive) {
      files ++ dir.listFiles.filter(_.isDirectory).flatMap(getFiles(_, recursive, format))
    } else {
      files
    }
  }

  /**
    * *Side Effect* sets logging level fof cli based on input args
    * @param config cli config class
    */
  def setLogLevel(config: Config): Unit = {
    var logLevel = Level.ERROR
    if (config.verbose) {
      logLevel = Level.INFO
    }
    if (config.debug) {
      logLevel = Level.DEBUG
    }
    LoggerFactory.getLogger(getClass.getName)
      .asInstanceOf[Logger]
      .setLevel(logLevel)
  }

  /**
    * traverse json object for property
    * // @tailrec
    * @param json object to traverse
    * @param parts list of path parts to traverse object with
    * @return
    */
  def traverseJson(json: JsValue, parts: ListBuffer[String]): JsValue = {
    val remaining = parts.length
    val key = parts.remove(0)
    val result = json(key)
    if (remaining == 1) {
      result
    } else {
      traverseJson(result, parts)
    }
  }


  /**
    * Main Application
    */
  OParser.parse(parser1, args, Config()) match {
    case Some(config: Config) =>
      setLogLevel(config)
      validateConfig(config)
      val files = getFiles(config.in, config.recursive, config.format).toList
      var bytesTracker: Long = 0
      var toCombine: ListBuffer[JsValue] = ListBuffer[JsValue]()
      var fileCntr = 0
      val totalFound = files.length
      var iterationCounter = 0
      logger.info(f"FOUND: $totalFound files")
      for (file ← files) {
        iterationCounter += 1

        var fileContent = config.format match {
          case "json" ⇒ Json.parse(Source.fromFile(file).mkString)
          case "xml" ⇒
            Try(XML.loadString(Source.fromFile(file).mkString).toJson) match {
              case Failure(e) ⇒
                logger.error(f"Unable to parse XML for ${file.toString}: ${e.toString}")
                Json.obj(
                  "error" → Json.obj("ex" → e.toString, "file" → file.toString)
                )
              case Success(value) ⇒ value
            }
        }

        if (config.element.nonEmpty) {
          val parts = config.element.to[ListBuffer]
          Try(traverseJson(fileContent, parts)) match {
            case Failure(e) ⇒ logger.error(f"Unable to find '${config.element.mkString(".")}' in file ${file.toString}")
            case Success(result) ⇒ fileContent = result
          }
        }

        val fileContentSize = fileContent.toString().getBytes.length

        if ((fileContentSize + bytesTracker) > config.size) {
          fileCntr += 1
          writeFile(config, toCombine.toList, fileCntr)
          bytesTracker = 0
          toCombine = ListBuffer[JsValue]()
        }

        toCombine += fileContent
        bytesTracker += fileContentSize

        logger.debug(f"FILE ${fileCntr + 1}: $bytesTracker/${config.size} bytes - file  $iterationCounter of $totalFound: ${file.toString}")

      }
      // cleanup final entries
      if (toCombine.nonEmpty) {
        fileCntr += 1
        writeFile(config, toCombine.toList, fileCntr)
      }
      logger.info(f"WRITTEN: $fileCntr files to  ${config.out.toString}")

    case _ => sys.exit(1)
  }

}
