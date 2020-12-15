# 【字节流、字符流、Properties】
### 第一章 字符流

##### 1.为什么要用字符流

```java
public class WhyUseFileReader {
    public static void main(String[] args) throws IOException {
        //1.创建FileInputStream对象
        FileInputStream fis = new FileInputStream("1.txt");
        //2.一次读取一个字节
        int b = 0;//用来保存读取的字节
        while ((b = fis.read()) != -1) {
            System.out.println((char) b);
        }
        //3.释放资源
        fis.close();
    }
}
文件的内容是: Ja中va 一共:1+1+2+1+1 = 6个字节
运行结果是:
	J
    a
    乱码
    乱码
    v
    a   
怎么解决这个乱码问题呢????
    使用字符流读取中文,字符流以字符为单位操作数据,Ja中va这个就是5个字符!!
```

##### 2.字符输入流

```java
顶层父类: Reader(抽象类)
共性方法:
	public void close();//释放资源
	public int read();//一次读一个字符
    public int read(char[] chs);//一次读取一堆字符,实际读取的字符个数
```

##### 3.FileReader类的使用

```java
文件的字符输入流: 以字符为单位从文件中读取数据
```

- a.构造方法

  ```java
  public FileReader(String filepath);//指定文件路径
  public FileReader(File file);//指定File对象
  
  public class FileReaderDemo01 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileReader对象
          FileReader fr = new FileReader("1.txt");
  //        FileReader fr = new FileReader(new File("1.txt"));
          /**
           * 以上构造干了三件事!!
           * a.创建对象 fr
           * b.判断文件是否存在
           *      如果存在,就存在,不清空内容
           *      如果不存在,直接抛出FileNotFoundException异常
           * c.绑定fr和文件1.txt
           */
      }
  }
  ```

- b.读取一个字符

  ```java
  /**
   * FileReader的read方法
   */
  public class FileReaderDemo02 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileReader对象
          FileReader fr = new FileReader("1.txt");
          //2.一次读一个字符
          //int ch = fr.read();
          //System.out.println((char) ch);
          //============一次读取一个字符的标准循环============
          int ch = 0; //用于保证读取到的字符
          /**
           * (ch = fr.read()) != -1
           * 以上代码干了三件事!
           * a.先读 fr.read()
           * b.赋值 ch = 读取到的字符
           * c.判断 ch != -1
           */
          while ((ch = fr.read()) != -1) {
              System.out.print((char) ch);
          }
          //3.释放资源
          fr.close();
      }
  }
  ```

- c.读取一个字符数组

  ```java
  /**
   * FileReader的read方法
   */
  public class FileReaderDemo03 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileReader对象
          FileReader fr = new FileReader("1.txt");
          //2.一次读一个字符数组
          //char[] chs = new char[4];
          //int len = fr.read(chs);
          //System.out.println("实际读取"+len+"个字符!");
          //System.out.println(new String(chs,0,len));
          //=========一次读取一个字符数组的标准循环==========
          char[] chs = new char[4];
          int len = 0;
          /**
           * (len = fr.read(chs)) != -1
           * 以上代码干了三件事!!
           * a.先读 fr.read(chs)
           * b.赋值 len = 实际读取的字符个数
           * c.判断 len != -1
           */
          while ((len = fr.read(chs)) != -1) {
              System.out.print(new String(chs,0,len));
          }
  
          //3.释放资源
          fr.close();
      }
  }
  ```

##### 4.字符输出流

```java
顶层父类:Writer(抽象类)
共性方法:
	public void close();//释放资源
	public void flush();//刷新缓冲区(对于字符流有用!!)
	
	public void write(int ch);//写一个字符
	public void write(char[] ch);//写一堆字符
	public void write(char[] ch,int startIndex,int len);//写一堆字符的一部分

	public void write(String str);//写一个字符串
	public void write(String str,int startIndex,int len);//写一个字符串的一部分
```

##### 5.FileWriter类的使用

```java
文件的字符输出流:向文件中以字符为单位写数据
```

- a.构造方法

  ```java
  public FileWriter(String filepath);指定文件路径
  public FileWriter(File file);指定文件对象
      
  /**
   * FileWriter的构造方法
   */
  public class FileWriterDemo01 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileWriter对象
          FileWriter fw = new FileWriter("1.txt");
  //        FileWriter fw = new FileWriter(new File("1.txt"));
          /***
           * 以上构造干了三件事!
           * a.创建对象fw
           * b.判断文件是否存在
           *      如果存在,清空文件内容
           *      如果不存在,创建一个新文件
           * c.绑定fw和1.txt
           */
      }
  }    
  ```

- b.写出字符数据的三组方法

  ```java
  public void write(int ch);//写一个字符
  
  public void write(char[] ch);//写一堆字符
  public void write(char[] ch,int startIndex,int len);//写一堆字符的一部分
  
  public void write(String str);//写一个字符串
  public void write(String str,int startIndex,int len);//写一个字符串的一部分
  /**
   * FileWriter的write方法
   */
  public class FileWriterDemo02 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileWriter对象
          FileWriter fw = new FileWriter("1.txt");
          //2.以字符写数据
          //a.一次写一个字符
          fw.write('中');
          //b.一次写一个字符数组
          char[] chs = {'中','国','我','爱','你'};
          fw.write(chs);
          //c.一次写一个字符数组中的一部分
          fw.write(chs,2,3);
          //d.一次写一个串
          fw.write("中国我爱你,你爱我吗?");
          //e.一次写一个串的一部分
          fw.write("中国我爱你,你爱我吗?",4,3);
          //3.释放资源
          fw.close();
      }
  }
  ```

- c.关闭和刷新的区别

  ```java
  /**
   * FileWriter的flush和close方法
   */
  public class FileWriterDemo03 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileWriter对象
          FileWriter fw = new FileWriter("1.txt");
          //2.写数据
          fw.write("Java");
          //调用刷新缓冲区
          //fw.flush();
          //3.释放资源
          fw.close();
          //再次写数据
          fw.write("PHP"); //报错!!
      }
  }
  
  close方法和flush方法的区别:
  	a.flush用于刷新缓冲区,流并不关闭,可以继续写数据!
      b.close也有刷新的功能,同时流也会关闭,不能继续使用该流写数据了!    
  ```

- d.续写和换行

  ```java
  如何续写呢?? 非常简单,只要使用以下两个构造即可
  public FileWriter(String filepath,boolean append);指定文件路径
  public FileWriter(File file,boolean append);指定文件对象 
      
  /**
   * FileWriter的续写
   */
  public class FileWriterDemo04 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileWriter对象
          FileWriter fw = new FileWriter("1.txt", true);//true表示续写
          //2.写数据
          for (int i = 0; i < 10; i++) {
              fw.write("java");
          }
          //3.释放资源
          fw.close();
      }
  } 
  
  如何换行??非常简单,只要写入一个代码换行符号即可
      windows: \r\n
      Linux: \n
      Mac: \r 
          
  /**
   * FileWriter的换行
   */
  public class FileWriterDemo05 {
      public static void main(String[] args) throws Exception {
          //1.创建一个FileWriter对象
          FileWriter fw = new FileWriter("1.txt");
          //2.写数据
          for (int i = 0; i < 10; i++) {
              fw.write("php\r\n");
  //            fw.write("\r\n");
          }
          //3.释放资源
          fw.close();
      }
  }  
  
  字符流和字节流我们什么时候采用哪些流??
      如果读写文本文件(.txt .java): 字符流
      如果其他文件(.png .avi .mp3 .ppt .doc .xlsx):字节流    
  ```

### 第二章 IO流的异常处理

##### 1.JDK7之前的标准IO处理

```java
/**
  * JDK7之前的处理方式
  */
public static void tryCatch01(){
    FileReader fr = null;
    try {
        fr = new FileReader("1.txt");
        int ch = fr.read();
    }catch (IOException ie){
        ie.printStackTrace();
    }finally {
        //释放资源的代码
        try {
            if (fr != null) {
                fr.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

##### 2.JDK7引入的IO处理

```java
/**
  * JDK7的处理方式
  * try-with-resource
  * 和资源一起try
  */
public static void tryCatch02(){
    try(FileReader fr = new FileReader("1.txt")){
        int ch = fr.read();
    }catch (IOException ie){
        ie.printStackTrace();
    }
}
```

### 第三章 Properties类

##### 1.Propertie类的介绍

```java
Properties 类表示了一个持久的属性集!
    什么是集: 它是一个集合
    什么是属性集: 其他就是键值对,本质就是一个Map集合  
    什么是持久的: Properties类具有和硬盘交互的方法
```

##### 2.构造方法

```java
public Properties(); 创建一个空属性的集合

public class PropertiesDemo01 {
    public static void main(String[] args) {
        //1.创建一个Properties对象
        Properties ps = new Properties();
        System.out.println(ps);
        //注意:Properties不需要写泛型,默认键和值都是String
    }
}    
```

##### 3.基本保存数据的方法

```java
public String setProperty(String key,String value);//添加/修改属性,类似于map的put方法
public String getProerty(String key);//根据属性名获取属性值,类似于map的get方法
public Set<String> stringPropertyNames();//获取集合中所有的属性名,类似于map的keySet

public class PropertiesDemo01 {
    public static void main(String[] args) {
        //1.创建一个Properties对象
        Properties ps = new Properties();
        System.out.println(ps);
        //2.添加数据
        ps.setProperty("java","100");//相当于map的put
        ps.setProperty("php","80");//相当于map的put
        ps.setProperty("c++","99");//相当于map的put
        ps.setProperty("python","98");//相当于map的put
        System.out.println(ps);
        //3.根据属性名获取属性值
        String propertyValue = ps.getProperty("java"); //相当于map的get
        System.out.println(propertyValue);
        //4.获取所有属性名的集合
        Set<String> propertyNames = ps.stringPropertyNames();//相当于map的keySet
        System.out.println(propertyNames);
    }
}
```

##### 4.与流相关的方法

```java
持久相关的方法,就是和流相关的方法

public void store(OutputStream/Writer out,String 注释内容); //保存数据  
public void load(InputStream/Reader in);//读取数据  

public class PropertiesDemo02 {
    public static void main(String[] args) throws IOException {
        //1.创建一个Properties对象
        Properties ps = new Properties();
        //2.添加数据
        ps.setProperty("java","100");//相当于map的put
        ps.setProperty("php","80");//相当于map的put
        ps.setProperty("c++","99");//相当于map的put
        ps.setProperty("python","98");//相当于map的put
        //3.保存数据
        ps.store(new FileOutputStream("1.txt"),"");
        //4.再创建一个Properties
        Properties ps1 = new Properties();
        System.out.println(ps1);
        //5.读取数据
        ps1.load(new FileInputStream("1.txt"));
        System.out.println(ps1);
    }
}
注意:
	Properties类保存的文件一般会起名叫xxx.properties(规范!!)
```

### **第四章** ResourceBundle工具类

##### 1.ResourceBundle类的介绍

```java
ResourceBundle这是一个抽象类,其子类PropertyResourceBundle可以用于读取.properties后缀的文件
PropertyResourceBundle可以直接读取"src"目录下的.properties文件,并且我们读取文件时,只需要写"文件名",不需要写文件的后缀名    
```

##### 2.ResourceBundle类对象的创建

```java
public class ResourceBundleDemo {
    public static void main(String[] args) {
        //properties文件在src目录下
        //读取时路径怎么写?? 不好写
        //ResourceBundle 专门用于读取src目录下(默认就是从src目录查找文件)
        //1.如何获取ResourceBundle对象
        ResourceBundle bundle = ResourceBundle.getBundle("1");
        System.out.println(bundle);
    }
}
```

##### 3.ResourceBundle读取配置文件操作

```java
public class ResourceBundleDemo {
    public static void main(String[] args) {
        //properties文件在src目录下
        //读取时路径怎么写?? 不好写
        //ResourceBundle 专门用于读取src目录下(默认就是从src目录查找文件)
        //1.如何获取ResourceBundle对象
        ResourceBundle bundle = ResourceBundle.getBundle("1");
        System.out.println(bundle);

        //2.通过ResourceBundle的getString
        String value1 = bundle.getString("username");
        System.out.println(value1);

        String value2 = bundle.getString("password");
        System.out.println(value2);
    }
}
```
