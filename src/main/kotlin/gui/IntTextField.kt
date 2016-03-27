package gui

import javafx.scene.control.TextField

public class IntTextField(var allowEmpty:Boolean = false):TextField()
{
    var min:Int = Int.MIN_VALUE

        set(newMin:Int)
        {
            field = newMin
            if (max < newMin) max = min
        }

    var max:Int = Int.MAX_VALUE

        set(newMax:Int)
        {
            field = newMax
            if (min > newMax) min = max
        }

    fun setValue(newValue:Int)
    {
        var clampedValue = Math.min(Math.max(newValue,min),max)
        super.replaceText(0,length,clampedValue.toString())
    }

    override fun replaceText(start:Int,end:Int,text:String)
    {
        val newText = getText(0,start)+text+getText(end,length)
        try
        {
            setValue(newText.toInt())
        }
        catch (e:NumberFormatException)
        {
            if(allowEmpty && newText.isEmpty())
            {
                super.replaceText(0,length,newText)
            }
        }
    }
}
