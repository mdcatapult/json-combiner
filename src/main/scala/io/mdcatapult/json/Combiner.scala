package io.mdcatapult.json

import ch.qos.logback.classic.{Level, Logger}
import com.typesafe.scalalogging.{LazyLogging}
import scopt.OParser
import java.io.{File, PrintWriter}
import org.slf4j.LoggerFactory
import scala.collection.mutable.ListBuffer
import scala.io.Source
import play.api.libs.json.{JsValue, Json, Xml}
import play.api.libs.json.implicits.JsonXmlImplicits._
import play.api.libs.json.Json
import scala.xml._

sealed case class Config(
                          in: File = new File("."),
                          out: File = new File("."),
                          size: Int = 16760832, // 16Mb - 16Kb
                          prefix: String = "",
                          recursive: Boolean = false,
                          verbose: Boolean = false,
                          debug: Boolean = false,
                          format: String = "json")

object Combiner extends App with LazyLogging {
  val builder = OParser.builder[Config]
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

  def setLogLevel(config: Config) = {
    var logLevel = Level.ERROR
    if (config.verbose) {
      logLevel = Level.INFO
    }
    if (config.debug) {
      logLevel = Level.DEBUG
    }
    LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).
      asInstanceOf[Logger].setLevel(logLevel)
  }


  OParser.parse(parser1, args, Config()) match {
    case Some(config: Config) => {
      setLogLevel(config)
      validateConfig(config)
      val files = getFiles(config.in, config.recursive, config.format).toList
      var bytesTracker: Long = 0
      var toCombine: ListBuffer[JsValue] = ListBuffer[JsValue]()
      var fileCntr = 0
      val totalFound = files.length
      var iterationCounter = 0
      logger.info(f"FOUND: ${totalFound} files")
      for (file ← files) {
        iterationCounter += 1

        val fileContent = config.format match {
          case "json" ⇒ Json.parse(Source.fromFile(file).mkString)
          case "xml" ⇒ XML.loadString(Source.fromFile(file).mkString).toJson
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

        logger.debug(f"FILE ${fileCntr + 1}: $bytesTracker/${config.size} bytes - file  $iterationCounter of $totalFound")

      }
      // cleanup final entries
      if (toCombine.nonEmpty) {
        fileCntr += 1
        writeFile(config, toCombine.toList, fileCntr)
      }
      logger.info(f"WRITTEN: $fileCntr files to  ${config.out.toString}")
    }
    case _ => sys.exit(1)
  }

}
