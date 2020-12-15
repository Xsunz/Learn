# 【ArrayList集合】


```java
	String
        构造方法:
			public String();
			public String(String str);
			public String(char[] chs);
			public String(byte[] bs);
			public String(byte[] bs,int startIndex,int len);
			直接赋值
            String name = "jack";    	
        成员方法:
			判断: equals,equalsIgnoreCase,contains,startsWith,endsWith
			获取: length,indexof,lastIndexof,substring,charAt,concat
			转换: toCharArray,getBytes,toLowerCase,toUpperCase,replace
			分割: split,trim
    StringBuilder
         构造方法:
			public StringBuilder();
			public StringBuilder(String str);
		 成员方法:
			public StringBuilder append(任意类型);
			public StringBuilder reverse();
			public String toString();
```



#### 第一章 ArrayList集合

```java
//对象的内存图
Dog 	dd 	    = 		new Dog("旺财",18);
    栈中:0xaaa		堆中: name="旺财",age=18	
//如果访问这个name和age属性
        dd.name(dd.getName())
        dd.age(dd.getAge())
结论:我们其实记住的对象的地址值,然后通过地址值访问该对象的属性或者调用方法   
```

##### 1.1 引入—对象数组

```java
需求:使用学生数组，存储三个学生对象
public class ArrayDemo {
    public static void main(String[] args) {
        //需求:使用学生数组，存储三个学生对象
        //1.定义一个学生数组
        Student[] students = new Student[3]; //null
        //2.保存三个学生对象
        Student s0 = new Student("jack",18);
        students[0] = s0;
        Student s1 = new Student("rose",19);
        students[1] = s1;
        Student s2 =  new Student("marry",20);
        students[2] = s2;
        //此时,数组中保存不是真的对象,保存对象的地址值
        //3.遍历数组
        for (int i = 0; i < students.length; i++) {
            Student s = students[i];
            System.out.println(s.getName()+"...."+s.getAge());
        }
    }
}    
```

##### 1.2 ArrayList类概述

```java
什么是ArrayList: 他就是一个大小可以改变的数组
```

##### 1.3 ArrayList类常用方法

```java
构造方法:
	public ArrayList();创建一个实际只有0个元素的集合(初始容量为10)
成员方法:
	增: 
		public boolean add(元素); 向集合最后面添加元素,返回值代表是否添加成功
		public void add(int index,元素);向集合指定位置添加元素
	删:
		public boolean remove(元素); 删除指定的元素,返回值代表是否删除成功
		public E remove(int index); 删除指定位置的元素,返回被删除的那个元素       
	改:
		public E set(int index,新元素); 将指定索引的元素改为新的元素,返回被修改的元素
	查:
		public int size(); 获取集合中实际元素的个数
        public E get(int index); 获取指定位置的元素   

public class ArrayListDemo01 {
    public static void main(String[] args) {
        //1.创建一个集合对象
        ArrayList<String> arr = new ArrayList<String>();
        //2.增
        arr.add("jack");
        arr.add("rose");
        arr.add("tom");
        System.out.println(arr);
        arr.add(1,"marry");
        System.out.println(arr);
        System.out.println("------------");
        //3.删
        boolean b = arr.remove("rose");
        System.out.println(b);
        System.out.println(arr);

        String remove = arr.remove(2);
        System.out.println(remove);
        System.out.println(arr);
        System.out.println("------------");
        //4.改
        String set = arr.set(0, "ckja");
        System.out.println(set);
        System.out.println(arr);
        System.out.println("------------");
        //5.查
        System.out.println(arr.size());
        String s = arr.get(1);
        System.out.println(s);
        System.out.println(arr);
    }
}            
```

##### 1.4 ArrayList存储字符串并遍历

```java
需求:创建一个存储字符串的集合，存储3个字符串元素，使用程序实现在控制台遍历该集合
    
public class ArrayListDemo92 {
    public static void main(String[] args) {
        //需求:创建一个存储字符串的集合，存储3个字符串元素，使用程序实现在控制台遍历该集合
        //1.创建一个存储字符串的集合
        ArrayList<String> arr = new ArrayList<String>();
        //2.存储3个字符串元素
        arr.add("java");
        arr.add("php");
        arr.add("c++");
        //3.在控制台遍历该集合
        for (int i = 0; i < arr.size(); i++) {
            String s = arr.get(i);
            System.out.println(s);
        }
    }
}    
```

##### 1.5 ArrayList存储学生对象并遍历

```java
需求:创建一个存储学生对象的集合，存储3个学生对象，使用程序实现在控制台遍历该集合
    
public class ArrayListDemo03 {
    public static void main(String[] args) {
        //需求:创建一个存储学生对象的集合，存储3个学生对象，使用程序实现在控制台遍历该集合
        //1.存储学生对象的集合
        ArrayList<Student> students = new ArrayList<Student>();
        //2.存储3个学生对象
        Student s1 = new Student("jack", 10);
        students.add(s1);

        Student s2 = new Student("rose", 20);
        students.add(s2);

        Student s3 = new Student("tom", 30);
        students.add(s3);
        //3.在控制台遍历该集合
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            System.out.println(s.getName()+"..."+s.getAge());
        }
    }
}
```

##### 1.6 ArrayList存储学生对象并遍历升级版

```java
需求:创建一个存储学生对象的集合，存储3个学生对象，使用程序实现在控制台遍历该集合。
    学生的姓名和年龄来自于键盘录入
    
public class ArrayListDemo04 {
    public static void main(String[] args) {
        //需求:创建一个存储学生对象的集合，存储3个学生对象，使用程序实现在控制台遍历该集合。
        //学生的姓名和年龄来自于键盘录入
        //1.创建一个存储学生对象的集合
        ArrayList<Student> students = new ArrayList<Student>();
        //2.存储3个学生对象,学生的姓名和年龄来自于键盘录入
        for (int i = 0; i < 3; i++) {
            Scanner sc = new Scanner(System.in);
            System.out.println("请输入姓名:");
            String name = sc.next();//next() nextLine();
            System.out.println("请输入年龄:");
            int age = sc.nextInt();
            //创建对象
            Student s = new Student(name,age);
            //添加到集合中
            students.add(s);
        }
       //3.在控制台遍历该集合。
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            System.out.println(s.getName()+"..."+s.getAge());
        }
    }
}    

public class ArrayListDemo04 {
    public static void main(String[] args) {
        //需求:创建一个存储学生对象的集合，存储3个学生对象，使用程序实现在控制台遍历该集合。
        //学生的姓名和年龄来自于键盘录入
        //1.创建一个存储学生对象的集合
        ArrayList<Student> students = new ArrayList<Student>();
        //2.存储3个学生对象,学生的姓名和年龄来自于键盘录入
        addStudent(students);
        //3.在控制台遍历该集合。
        printStudents(students);
    }

    //打印集合
    public static void printStudents(ArrayList<Student> students) {
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            System.out.println(s.getName() + "..." + s.getAge());
        }
    }

    //添加学生
    public static void addStudent(ArrayList<Student> students) {
        for (int i = 0; i < 3; i++) {
            Scanner sc = new Scanner(System.in);
            System.out.println("请输入姓名:");
            String name = sc.next();//next() nextLine();
            System.out.println("请输入年龄:");
            int age = sc.nextInt();
            //创建对象
            Student s = new Student(name, age);
            //添加到集合中
            students.add(s);
        }
    }
}
```

##### 1.7 ArrayList存储基本数据类型

```java
需求: 使用集合保存基本类型的数据,并遍历打印到控制台
    
a。集合中的泛型不能是基本类型,只能使用引用类型
    ArrayList<int> ArrayList<double> 这么是错误的
b.Java为8中基本类型,提供了对应的引用类型,我们一般称为基本类型的包装类,简称包装类
    byte		Byte
    short		Short
    char		Character
    int			Integer
    long		Long
    float		Float
    double		Double
    boolean		Boolean
 
public class ArrayListDemo06 {
    public static void main(String[] args) {
        //需求: 使用集合保存基本类型的数据,并遍历打印到控制台
        //1.创建集合保存基本类型的数据
        ArrayList<Integer> arr = new ArrayList<Integer>();
        //2.添加数据
        arr.add(1); 
        arr.add(2);
        arr.add(3);
        arr.add(4);
        arr.add(5);
        //3.遍历
        for (int i = 0; i < arr.size(); i++) {
            int num = arr.get(i);
            System.out.println(num);
        }
//        System.out.println(arr);
//        arr.remove(2); //删除指定位置的元素
//        System.out.println(arr);
    }
}   
```

##### 总结

```java
能够知道集合和数组的区别
    a.数组一旦创建其长度是固定的,集合的长度是随时可以变化的
    b.数组的元素是任意类型,集合的泛型必须是引用类型
能够使用ArrayList集合中常用的方法    
    a.构造方法:
	ArrayList<引用类型> arr = new ArrayList<引用类型>();//创建一个实际元素个数0,容量为10的集合
	b.成员方法:
	增:
		排队:add(E e)  插队:add(int index,E e);
	删:
		直接删元素:remove(E e) 根据索引删除元素:remove(int index)
	改:
		set(int index,E e);
	查:
		E get(int index);
		int size();
		数组.length 字符串.length() 集合.size() stream流.count()
能够使用ArrayList集合存储数据[至少独立完成1遍]
能够使用ArrayList集合存储字符串并遍历[至少独立完成1遍]
能够使用ArrayList集合存储自定义对象并遍历[至少独立完成1遍]
```

##### 扩展

```java
1.String的扩展
    等效性: "abc" ===> {'a','b','c'} String底层其实保存数据的是一个数组
    内存:
		通过new创建的字符串: 保存堆区
        直接赋值字符串常量 : 保存常量池
            

public class StringDemo {
    public static void main(String[] args) {
        //1.创建几个字符串
        String s1 = "abc"; //字符串常量
        String s2 = "a"+"bc"; //字符串常量相加
        String s3 = "ab"+"c"; //字符串常量相加
        String s4 = "a"+"b"+"c"; //字符串常量相加
        //2.地址值
        System.out.println(s1 == s2); //true
        System.out.println(s1 == s3); //true
        System.out.println(s1 == s4); //true
        System.out.println("=================");
        //结论1:以上都是true,字符串常量和字符串常量相加,得到字符串对象都是在常量池中
        //并且常量池中的常量具有共享性(内容一样的字符串常量,只保存一份)
        //3.其他情况
        String s = "ab";
        String ss = "bc";

        String s5 = new String("abc");
        String s6 = "a"+ ss;
        String s7 = s + "c";
        String s8 = new String("a") + ss;
        String s9 = s + new String("c"); 
        
        System.out.println(s5 == s6);
        System.out.println(s5 == s7);
        System.out.println(s5 == s8);
        System.out.println(s5 == s9);
        //结论2:除了字符串常量和字符串常量相加,其他情况下得到的字符串,无论内容是否相同
        //地址肯定是不一样,也就是说他们都是新的对象
    }
}

2.处理几个习题
  题目a:定义方法，获取一个包含4个字符的验证码，每一位字符是随机选择的字母和数字，可包含a-z,A-Z,0-9  
  public class TestDemo01 {
        public static void main(String[] args) {
            //题目a:定义方法，获取一个包含4个字符的验证码，每一位字符是随机选择的字母和数字，可包含a-z,A-Z,0-9
            //获取验证吗
            String code = getCode(-4);
            System.out.println("验证码:"+code);
        }
        //生成验证码
        public static String getCode(int number){
            //合法性判断
            if (number <= 0) {
                System.out.println("个数有误~~");
                return "error";
            }


            Random rd = new Random();
            //1.创建一个数组
            char[] chs = new char[62];
            //定义一个索引
            int index = 0;
            for (char i = 'a'; i <= 'z'; i++) {
                chs[index++] = i;
            }
            for (char i = 'A'; i <= 'Z'; i++) {
                chs[index++] = i;
            }
            for (char i = '0'; i <= '9'; i++) {
                chs[index++] = i;
            }
            //2.生成验证码
            StringBuilder yzm = new StringBuilder();
            for (int i = 0; i < number; i++) {
                int charIndex = rd.nextInt(chs.length);
                char ch = chs[charIndex];
                yzm.append(ch);
            }
            //3.打印
            String code = yzm.toString();
            //4.返回
            return code;
        }
    }

题目b:请编写程序模拟验证码的判断过程，如果输入正确，给出提示，结束程序。如果输入错误，给出提示，验证码刷新，重新输入，直至正确为止
    public class TestDemo02 {
    public static void main(String[] args) {
        //题目b:请编写程序模拟验证码的判断过程，如果输入正确，给出提示，结束程序。
        //如果输入错误，给出提示，验证码刷新，重新输入，直至正确为止
        while (true){
            //1.显示验证码给用户看
            String code = getCode(4);
            System.out.println("您的验证码是:" + code);
            //2.提示用户输入
            System.out.println("请输入您看到的验证码:");
            String userCode = new Scanner(System.in).next();
            //3.判断
            if (code.equalsIgnoreCase(userCode)) {
                System.out.println("验证码OK");
                break;
            } else {
                System.out.println("验证码NO");
            }
        }
    }
    //生成验证码
    public static String getCode(int number){
        //合法性判断
        if (number <= 0) {
            System.out.println("个数有误~~");
            return "error";
        }
        Random rd = new Random();
        //1.创建一个数组
        char[] chs = new char[62];
        //定义一个索引
        int index = 0;
        for (char i = 'a'; i <= 'z'; i++) {
            chs[index++] = i;
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            chs[index++] = i;
        }
        for (char i = '0'; i <= '9'; i++) {
            chs[index++] = i;
        }
        //2.生成验证码
        StringBuilder yzm = new StringBuilder();
        for (int i = 0; i < number; i++) {
            int charIndex = rd.nextInt(chs.length);
            char ch = chs[charIndex];
            yzm.append(ch);
        }
        //3.打印
        String code = yzm.toString();
        //4.返回
        return code;
    }
}

题目c:判断号码为18位，不能以数字0开头，前17位只可以是数字，最后一位可以是数字或者大写字母X。
    
public class TestDemo03 {
    public static void main(String[] args) {
//        题目c:判断号码为18位，不能以数字0开头，前17位只可以是数字，最后一位可以是数字或者大写字母X。
        boolean b = checkId("113123197804053374843985Y");
        System.out.println(b);
    }

    public static boolean checkId(String id) {
        //1.判断号码为18位
        if (id.length() != 18) {
            return false;
        }
        //2.不能以数字0开头
        if (id.charAt(0) == '0'){
            return false;
        }
        //3.前17位只可以是数字
        for (int i = 0; i < id.length() - 1; i++) {
            char ch = id.charAt(i);
            if (ch<'0' || ch >'9'){
                return false;
            }
        }
        //4.最后一位可以是数字或者大写字母X  
        char lastChar = id.charAt(17);
        if ((lastChar < '0' || lastChar > '9') && lastChar != 'X'){
            return false;
        }
        return true;
    }
}
 
题目d:"Java语言是面向对象的，Java语言是健壮的，Java语言是安全的，Java是高性能的，Java语言是跨平台的"。请编写程序，统计该文本中"Java"一词出现的次数。
    
public class TestDemo04 {
    public static void main(String[] args) {
        //题目d:"Java语言是面向对象的，Java语言是健壮的，Java语言是安全的，Java是高性能的，Java语言是跨平台的"。
        //请编写程序，统计该文本中"Java"一词出现的次数。
        //1.字符串
        String str = "Java语言是面向对象的，Java语言是健壮的，Java语言是安全的，Java是高性能的，Java语言是跨平台的";
        //2.先找Java
        //计数器
        int count = 0;
        while (true){
            int index = str.indexOf("Java");
            if (index >= 0) {
                count++;
                //3.截取Java以后的那一段
                str = str.substring(index+4);
            }else{
                break;
            }
        }
        //3.打印
        System.out.println(count);
    }
}  

题目e:day06练习题14
    
public class TestDemo05 {
    public static void main(String[] args) {
        //1.提示输入
        System.out.println("请输入姓名年龄性别,需要遵循一下格式(姓名,年龄,性别):");
        String str = new Scanner(System.in).nextLine();
        //2.处理调用空格
        System.out.println(str);
        str = str.replace(" ","");
        System.out.println(str);
        //3.分割
        String[] ss = str.split(",");
        String name = ss[0];
        String age = ss[1];
        String sex = ss[2];
        //4.创建学生对象
        Student s = new Student(name,age,sex.charAt(0));
        //5.打印
        System.out.println(s.getName());
        System.out.println(s.getAge());
        System.out.println(s.getGender());
    }
}
```

