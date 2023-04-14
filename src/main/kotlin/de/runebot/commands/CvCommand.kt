package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import org.opencv.core.Mat
import org.opencv.highgui.HighGui
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage

object CvCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("cv")
    override val shortHelpText: String
        get() = "OpenCV test command."
    override val longHelpText: String
        get() = shortHelpText

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val image = Imgcodecs.imread(WhatCommand::class.java.getResource("/IDSB.jpg")?.path)
        val edges = Mat()
        Imgproc.Canny(image, edges, 125.0, 3.0)
        edges.copyTo(image)
        Util.sendImage(event.message.channel, "response.png", HighGui.toBufferedImage(image) as BufferedImage)
    }
}