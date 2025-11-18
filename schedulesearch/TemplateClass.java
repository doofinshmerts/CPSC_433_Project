package schedulesearch;
import java.util.Random;

public class TemplateClass
{
    static final int ARRAY_SIZE = 140;
    private int[] somearray;
    private String somestring;

    public TemplateClass(String _string)
    {
        System.out.println("starting template class");
        
        somestring = _string;
        
        Random rand = new Random();
        
        somearray = new int[ARRAY_SIZE];
        for(int i = 0; i < ARRAY_SIZE; i++)
        {
            somearray[i] = rand.nextInt(50);
        }
    }
    
    public void PrintProperties()
    {
        System.out.println(String.format("my name is %1$10s", somestring));

        for(int i = 0; i < ARRAY_SIZE; i++)
        {
            System.out.println(String.format("number at index: %1$3d, is %2$3d", i, somearray[i]));
        }
    }
}
