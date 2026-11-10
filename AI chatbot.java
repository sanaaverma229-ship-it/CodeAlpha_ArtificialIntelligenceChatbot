import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class Main extends JFrame {
    private final JTextArea chat = new JTextArea();
    private final JTextField input = new JTextField();
    private final Chatbot bot = new Chatbot();

    public Main() {
        setTitle("Smart AI Chatbot - Java Project");
        setSize(700, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        chat.setEditable(false);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        chat.setFont(new Font("Arial", Font.PLAIN, 15));
        JButton send = new JButton("Send");
        send.setBackground(new Color(35, 105, 180));
        send.setForeground(Color.WHITE);
        send.setOpaque(true);
        send.setBorderPainted(false);
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        main.add(new JLabel("SMART AI CHATBOT", SwingConstants.CENTER), BorderLayout.NORTH);
        main.add(new JScrollPane(chat), BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        add(main);
        send.addActionListener(e -> sendMessage());
        input.addActionListener(e -> sendMessage());
        show("Bot", "Hello! I know tech, science, geography, India, history, commerce and maths. "
                + "I can also calculate, tell time, and learn new things. Type 'help' to see how.");
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        input.setText("");
        show("You", text);
        show("Bot", bot.getResponse(text));
    }

    private void show(String who, String msg) {
        chat.append(who + ": " + msg + "\n\n");
        chat.setCaretPosition(chat.getDocument().getLength());
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {   // no screen: online compiler / server
            runConsole();
            return;
        }
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    private static void runConsole() {
        Chatbot bot = new Chatbot();
        Scanner in = new Scanner(System.in);
        System.out.println("=== SMART AI CHATBOT (console mode) - type 'exit' to quit ===");
        if (!in.hasNextLine()) {   // online compilers often give no keyboard input: show a demo
            System.out.println("(No input given, showing a demo)");
            for (String q : new String[]{"hi", "my name is Shobhit", "what is java", "who created python",
                    "capital of japan", "25% of 480", "(12 + 8) * 3 - sqrt 144", "what is gst", "explain photosynthsis",
                    "who is the first prime minister of india", "tell me a joke", "teach: what is zorb = Zorb is a test word",
                    "what is zorb", "what is my name"}) {
                System.out.println("You: " + q);
                System.out.println("Bot: " + bot.getResponse(q));
            }
            return;
        }
        System.out.print("You: ");
        while (in.hasNextLine()) {
            String q = in.nextLine().trim();
            if (q.equalsIgnoreCase("exit") || q.equalsIgnoreCase("quit")) break;
            if (!q.isEmpty()) System.out.println("Bot: " + bot.getResponse(q));
            System.out.print("You: ");
        }
        System.out.println("Bot: Goodbye!");
    }

    // =====================================================
    //  CHATBOT BRAIN
    // =====================================================
    static class Chatbot {

        static class Fact {
            String shown, answer;
            List<Set<String>> forms = new ArrayList<>();
        }

        private static final Set<String> STOP = new HashSet<>(Arrays.asList(
                "what", "is", "are", "was", "were", "the", "an", "a", "of", "for", "me", "tell", "about",
                "explain", "define", "does", "do", "did", "to", "in", "and", "how", "please", "can", "you",
                "who", "when", "where", "why", "which", "it", "its", "s", "t", "i", "give", "know", "mean",
                "meaning", "short", "some", "info", "information", "on", "use", "uses", "used"));

        private static final Map<String, String> SYN = new HashMap<>();
        static {
            for (String w : new String[]{"invented", "invent", "inventor", "creator", "created", "create", "founded",
                    "founder", "developed", "developer", "discovered", "discover", "discoverer", "author", "wrote", "made"})
                SYN.put(w, "create");
            SYN.put("nation", "nation");
        }

        private final List<Fact> kb = new ArrayList<>();
        private final Map<String, Double> idf = new HashMap<>();
        private double maxIdf = 1;
        private final Random rnd = new Random();
        private String userName = null;
        private final Path memFile = Paths.get("chatbot_memory.txt");

        private static final String[] JOKES = {
            "Why do programmers prefer dark mode? Because light attracts bugs.",
            "There are 10 types of people: those who understand binary and those who don't.",
            "A SQL query walks into a bar, walks up to two tables and asks: 'Can I join you?'",
            "How many programmers does it take to change a light bulb? None, that's a hardware problem.",
            "Why was the computer cold? It left its Windows open."};

        private static final String[] FUN = {
            "Java was originally called Oak.",
            "The first computer 'bug' was a real moth found in a Harvard Mark II computer in 1947.",
            "A day on Venus is longer than a year on Venus.",
            "Octopuses have three hearts.",
            "Bananas are botanically berries, but strawberries are not."};

        // "question phrases separated by ; | answer"
        private static final String[] DATA = {
            // ---- TECHNOLOGY ----
            "what is java; java language; java programming|Java is a high-level, object-oriented programming language known for 'write once, run anywhere': code compiles to bytecode that runs on the Java Virtual Machine (JVM).",
            "who created java; who invented java; creator of java|Java was created by James Gosling and his team at Sun Microsystems and released in 1995. Oracle owns it today.",
            "what is jvm; java virtual machine|The JVM (Java Virtual Machine) runs Java bytecode, which makes Java programs platform independent.",
            "what is jdk; what is jre; difference between jdk jre jvm|JDK = tools to develop Java (compiler + JRE). JRE = what you need to run Java programs. JVM = the engine inside the JRE that executes bytecode.",
            "what is python; python language|Python is a popular, easy-to-read high-level language used for web development, data science, AI and automation.",
            "who created python; who invented python; creator of python|Python was created by Guido van Rossum and first released in 1991.",
            "what is c programming; c language|C is a general-purpose language created by Dennis Ritchie at Bell Labs in the early 1970s. It is the base of many languages and operating systems.",
            "what is c++; cpp; c plus plus|C++ extends C with object-oriented features. It was created by Bjarne Stroustrup.",
            "what is javascript; js language|JavaScript is the programming language of the web. It makes web pages interactive and was created by Brendan Eich in 1995.",
            "what is html; hypertext markup language|HTML (HyperText Markup Language) structures web pages using elements such as headings, paragraphs, links and images.",
            "what is css; cascading style sheets|CSS (Cascading Style Sheets) controls how HTML looks: colours, fonts, spacing and layout.",
            "what is sql; structured query language|SQL (Structured Query Language) is used to create, read, update and delete data in relational databases.",
            "what is database; what is dbms|A database is an organized collection of data stored electronically. A DBMS (e.g. MySQL, Oracle) manages it.",
            "what is oop; object oriented programming|OOP (Object-Oriented Programming) organizes code into classes and objects. Its four pillars are encapsulation, inheritance, polymorphism and abstraction.",
            "what is class; class in java|A class is a blueprint that defines the data (fields) and behaviour (methods) of objects.",
            "what is object; object in java|An object is an instance of a class: a real thing created from the blueprint, with its own data.",
            "what is inheritance|Inheritance lets one class (child) reuse and extend the fields and methods of another class (parent). In Java it uses the 'extends' keyword.",
            "what is polymorphism|Polymorphism means 'many forms': the same method name can behave differently, through overloading (same class) or overriding (child class).",
            "what is encapsulation|Encapsulation bundles data and methods together and hides internal details using private fields with public getters and setters.",
            "what is abstraction|Abstraction hides complex details and shows only what is necessary, using abstract classes and interfaces.",
            "what is algorithm|An algorithm is a step-by-step procedure for solving a problem.",
            "what is data structure|A data structure is a way of organizing data so it can be used efficiently, e.g. arrays, lists, stacks, queues, trees and graphs.",
            "what is array|An array stores a fixed number of items of the same type in contiguous memory, accessed by index starting from 0.",
            "what is linked list|A linked list is a chain of nodes where each node holds data and a reference to the next node.",
            "what is stack|A stack is a LIFO (last in, first out) structure. Operations: push, pop, peek.",
            "what is queue|A queue is a FIFO (first in, first out) structure. Items are added at the rear and removed from the front.",
            "what is recursion|Recursion is when a function calls itself to solve smaller versions of a problem. It needs a base case to stop.",
            "what is api; application programming interface|An API (Application Programming Interface) lets different software programs talk to each other.",
            "what is git; version control|Git is a version-control system that tracks changes to code and lets teams collaborate. It was created by Linus Torvalds.",
            "what is github|GitHub is a website that hosts Git repositories so people can store, share and collaborate on code.",
            "what is linux|Linux is a free, open-source operating system kernel. Many servers, Android phones and supercomputers run on it.",
            "who created linux; who invented linux|Linux was created by Linus Torvalds in 1991.",
            "what is operating system; what is os|An operating system manages a computer's hardware and software and lets you run programs. Examples: Windows, Linux, macOS, Android.",
            "what is cpu; central processing unit; processor|The CPU (Central Processing Unit) is the 'brain' of the computer: it executes instructions and processes data.",
            "what is ram; random access memory|RAM (Random Access Memory) is fast, temporary memory that holds data for programs that are running. It is erased when the power is off.",
            "what is rom; read only memory|ROM (Read-Only Memory) is permanent memory that keeps its contents without power, e.g. the startup firmware.",
            "what is gpu; graphics card|A GPU (Graphics Processing Unit) is a processor designed for graphics and parallel work, and is widely used for gaming and AI.",
            "what is hardware|Hardware is the physical parts of a computer, such as the keyboard, monitor, CPU and hard disk.",
            "what is software|Software is the set of programs and instructions that tell a computer what to do.",
            "what is compiler; compiler vs interpreter; what is interpreter|A compiler translates a whole program into machine code before it runs; an interpreter translates and runs it line by line.",
            "what is variable|A variable is a named memory location that stores a value which can change.",
            "what is loop; for loop; while loop|A loop repeats a block of code while a condition is true. Common loops: for, while, do-while.",
            "what is function; what is method|A function (method) is a reusable block of code that performs a task and can take inputs and return a result.",
            "what is bug; what is debugging|A bug is an error in a program. Debugging is the process of finding and fixing bugs.",
            "what is internet|The Internet is a worldwide network that connects billions of computers and devices.",
            "what is world wide web; what is www; who invented www; who invented the web|The World Wide Web is the system of linked web pages accessed through the Internet. It was invented by Tim Berners-Lee in 1989.",
            "what is ip address|An IP address is a unique number that identifies a device on a network, e.g. 192.168.1.1.",
            "what is http; what is https|HTTP is the protocol used to transfer web pages. HTTPS is the secure, encrypted version.",
            "what is wifi; wi-fi|Wi-Fi is wireless networking technology that connects devices to a network or the Internet using radio waves.",
            "what is cloud computing|Cloud computing delivers services like storage, servers and software over the Internet, usually pay-as-you-go. Examples: AWS, Azure, Google Cloud.",
            "what is cybersecurity; cyber security|Cybersecurity protects computers, networks and data from attacks, theft and damage.",
            "what is encryption|Encryption converts data into a coded form so that only people with the right key can read it.",
            "what is blockchain|A blockchain is a shared digital ledger made of linked blocks of records that is very hard to alter. It underlies cryptocurrencies like Bitcoin.",
            "what is bitcoin; what is cryptocurrency|Bitcoin is the first cryptocurrency, a digital currency that works without a central bank. It was created in 2009 by the person or group called Satoshi Nakamoto.",
            "what is ai; what is artificial intelligence|Artificial Intelligence (AI) is the ability of machines to do tasks that normally need human intelligence, such as understanding language, recognizing images and making decisions.",
            "what is machine learning; what is ml|Machine Learning is a branch of AI where computers learn patterns from data instead of being explicitly programmed.",
            "what is deep learning|Deep learning is a type of machine learning that uses many-layered neural networks. It powers image recognition and language models.",
            "what is neural network|A neural network is a computing model inspired by the brain, made of layers of connected nodes that learn from data.",
            "what is nlp; natural language processing|NLP (Natural Language Processing) helps computers understand, interpret and generate human language.",
            "what is chatbot|A chatbot is a program that talks with users through text or voice. Some are rule-based; others use AI.",
            "what is data science|Data science uses statistics, programming and analysis to extract useful insights from data.",
            "what is big data|Big data means extremely large and complex data sets that need special tools to store and analyse.",
            "what is iot; internet of things|The Internet of Things (IoT) is a network of everyday devices, such as smart bulbs and watches, that connect to the Internet and share data.",
            "what is robotics; what is robot|Robotics is the field of designing and building robots, machines that can sense and act automatically.",
            "what is binary; binary number system|Binary is the base-2 number system that computers use. It has only two digits: 0 and 1.",
            "what is bit; what is byte|A bit is a single 0 or 1. A byte is 8 bits. 1 KB = 1024 bytes.",
            "what is open source|Open-source software has source code that anyone can see, use, modify and share, e.g. Linux and Firefox.",
            "who founded microsoft; who created microsoft|Microsoft was founded in 1975 by Bill Gates and Paul Allen.",
            "who founded apple; who created apple|Apple was founded in 1976 by Steve Jobs, Steve Wozniak and Ronald Wayne.",
            "who founded google; who created google|Google was founded in 1998 by Larry Page and Sergey Brin.",
            "who invented computer; father of computer|Charles Babbage is called the 'father of the computer' for designing the Analytical Engine in the 1800s. Modern electronic computers came in the 1940s.",
            "what is claude; who created claude; what is anthropic|Claude is an AI assistant made by Anthropic.",
            // ---- SCIENCE ----
            "what is gravity|Gravity is the force that attracts objects with mass toward each other. On Earth it gives objects an acceleration of about 9.8 m/s^2.",
            "what is photosynthesis|Photosynthesis is how plants make food: 6CO2 + 6H2O + light energy -> C6H12O6 (glucose) + 6O2, using chlorophyll in their leaves.",
            "what is atom|An atom is the smallest unit of an element. It has a nucleus of protons and neutrons with electrons around it.",
            "what is dna|DNA (deoxyribonucleic acid) is the molecule that carries genetic information. It has a double-helix shape.",
            "speed of light|Light travels at about 299,792 km per second (roughly 3 x 10^8 m/s) in a vacuum.",
            "speed of sound|Sound travels at about 343 m/s in air at 20 degrees C, faster in water and solids.",
            "boiling point of water; freezing point of water|Water boils at 100 degrees C (212 F) and freezes at 0 degrees C (32 F) at sea level.",
            "what are newton's laws; newton laws of motion|1) An object stays at rest or in uniform motion unless a force acts on it. 2) Force = mass x acceleration. 3) Every action has an equal and opposite reaction.",
            "what is ohm's law; ohm law|Ohm's law: V = I x R, where V is voltage, I is current and R is resistance.",
            "how many planets are there; planets in solar system|There are 8 planets: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus and Neptune.",
            "largest planet; biggest planet|Jupiter is the largest planet in our solar system.",
            "red planet; which planet is called red planet|Mars is called the Red Planet because of its iron-oxide (rusty) surface.",
            "closest planet to the sun; smallest planet|Mercury is the closest planet to the Sun and also the smallest.",
            "what is the sun; sun star|The Sun is the star at the centre of our solar system. It is a huge ball of hot gas, mostly hydrogen and helium, about 150 million km from Earth.",
            "distance to the moon; how far is the moon|The Moon is about 384,400 km from Earth.",
            "age of the earth; how old is the earth|Earth is about 4.5 billion years old.",
            "what is black hole|A black hole is a region of space where gravity is so strong that nothing, not even light, can escape.",
            "what is big bang|The Big Bang theory says the universe began from an extremely hot, dense state about 13.8 billion years ago and has been expanding since.",
            "how many elements are in the periodic table; who invented periodic table|The periodic table has 118 known elements. Dmitri Mendeleev created the first widely accepted version in 1869.",
            "chemical formula of water; what is water made of|Water is H2O: two hydrogen atoms and one oxygen atom.",
            "how many bones in the human body|An adult human has 206 bones.",
            "largest organ of the human body|The skin is the largest organ of the human body.",
            "normal body temperature|Normal human body temperature is about 37 degrees C (98.6 F).",
            "what are blood groups; blood types|The main blood groups are A, B, AB and O, each of which can be Rh positive or negative.",
            "what is evolution|Evolution is the process by which species change over many generations through natural selection. Charles Darwin described it in 1859.",
            "what is energy|Energy is the ability to do work. It exists in forms like kinetic, potential, heat, light and electrical, and is measured in joules.",
            "what is electricity|Electricity is the flow of electric charge, usually electrons through a conductor.",
            // ---- MATHS ----
            "value of pi; what is pi|Pi is about 3.14159. It is the ratio of a circle's circumference to its diameter.",
            "what is prime number|A prime number has exactly two factors: 1 and itself, e.g. 2, 3, 5, 7, 11. Note that 2 is the only even prime.",
            "pythagoras theorem; pythagorean theorem|In a right-angled triangle, a^2 + b^2 = c^2, where c is the hypotenuse.",
            "area of circle; circumference of circle|Area = pi x r^2. Circumference = 2 x pi x r.",
            "area of triangle; area of rectangle; area of square|Triangle = 1/2 x base x height. Rectangle = length x breadth. Square = side x side.",
            "quadratic formula|For ax^2 + bx + c = 0, x = (-b +/- sqrt(b^2 - 4ac)) / 2a.",
            "what is factorial|The factorial of n (n!) is the product of all positive integers up to n. For example 5! = 5 x 4 x 3 x 2 x 1 = 120.",
            "simple interest formula; compound interest formula|Simple interest = P x R x T / 100. Compound amount = P x (1 + R/100)^T.",
            "percentage formula; how to calculate percentage|Percentage = (part / whole) x 100.",
            // ---- GEOGRAPHY ----
            "capital of india|The capital of India is New Delhi.",
            "capital of france|The capital of France is Paris.",
            "capital of japan|The capital of Japan is Tokyo.",
            "capital of usa; capital of united states; capital of america|The capital of the United States is Washington, D.C.",
            "capital of uk; capital of england; capital of united kingdom|The capital of the United Kingdom is London.",
            "capital of china|The capital of China is Beijing.",
            "capital of australia|The capital of Australia is Canberra.",
            "capital of canada|The capital of Canada is Ottawa.",
            "capital of germany|The capital of Germany is Berlin.",
            "capital of russia|The capital of Russia is Moscow.",
            "capital of pakistan|The capital of Pakistan is Islamabad.",
            "capital of nepal|The capital of Nepal is Kathmandu.",
            "capital of bangladesh|The capital of Bangladesh is Dhaka.",
            "highest mountain; tallest mountain; mount everest|Mount Everest, on the Nepal-China border, is the highest mountain above sea level at about 8,849 m.",
            "largest ocean; biggest ocean|The Pacific Ocean is the largest and deepest ocean.",
            "how many continents|There are 7 continents: Asia, Africa, North America, South America, Antarctica, Europe and Australia (Oceania).",
            "longest river|The Nile (about 6,650 km) is traditionally considered the longest river, though some measurements say the Amazon is longer.",
            "largest country; biggest country|Russia is the largest country by area. India is the most populous country.",
            "smallest country|Vatican City is the smallest country in the world.",
            "largest desert; biggest desert|Antarctica is the largest desert overall; the Sahara is the largest hot desert.",
            "deepest point in the ocean; mariana trench|The Mariana Trench in the Pacific Ocean is the deepest place, at about 10,900 m.",
            "tallest building|The Burj Khalifa in Dubai is the tallest building, at 828 m.",
            "how many countries are there|There are 193 member states of the United Nations.",
            // ---- INDIA ----
            "independence day of india; when did india get independence|India became independent from British rule on 15 August 1947.",
            "republic day of india; when is republic day|India celebrates Republic Day on 26 January, because the Constitution came into effect on that day in 1950.",
            "who is gandhi; mahatma gandhi; father of the nation|Mohandas Karamchand Gandhi (Mahatma Gandhi) led India's non-violent freedom movement and is called the Father of the Nation.",
            "first prime minister of india|Jawaharlal Nehru was India's first Prime Minister.",
            "first president of india|Dr. Rajendra Prasad was the first President of India.",
            "who wrote the indian constitution; who is father of indian constitution|The Constitution was drafted by a committee chaired by Dr. B. R. Ambedkar. It was adopted on 26 November 1949 and came into effect on 26 January 1950.",
            "how many states are there in india|India has 28 states and 8 union territories.",
            "national animal of india|The national animal of India is the Bengal tiger.",
            "national bird of india|The national bird of India is the Indian peacock.",
            "national flower of india|The national flower of India is the lotus.",
            "national anthem of india; who wrote national anthem|Jana Gana Mana is India's national anthem, written by Rabindranath Tagore. The national song is Vande Mataram by Bankim Chandra Chatterjee.",
            "currency of india; indian currency|The currency of India is the Indian rupee. The Reserve Bank of India (RBI) issues it.",
            "who built the taj mahal; taj mahal|The Taj Mahal in Agra was built by Mughal emperor Shah Jahan (1632-1653) in memory of his wife Mumtaz Mahal.",
            "what is isro; chandrayaan; mangalyaan|ISRO is the Indian Space Research Organisation. Mangalyaan (Mars Orbiter Mission) reached Mars in 2014, and Chandrayaan-3 landed near the Moon's south pole in August 2023.",
            "longest river in india; ganga river|The Ganga (Ganges) is the longest river in India, about 2,525 km.",
            "highest mountain in india; kanchenjunga|Kanchenjunga (8,586 m) is the highest peak in India and the third highest in the world.",
            "who is apj abdul kalam; missile man|Dr. A. P. J. Abdul Kalam was India's 11th President (2002-2007) and a scientist known as the 'Missile Man of India'.",
            "current prime minister of india; current president of india; who is the prime minister; who is the president of usa; president of america; us president; president of the united states; current president|Office holders change, so I can't be sure my information is current. Please check an official government source.",
            // ---- HISTORY AND PEOPLE ----
            "when did world war 1 start; world war 1|World War I lasted from 1914 to 1918.",
            "when did world war 2 start; world war 2|World War II lasted from 1939 to 1945.",
            "who discovered america; columbus|Christopher Columbus reached the Americas in 1492, though Indigenous peoples had lived there for thousands of years and Vikings had arrived earlier.",
            "first man on the moon; moon landing|Neil Armstrong was the first person on the Moon, on 20 July 1969 during NASA's Apollo 11 mission.",
            "who invented telephone; telephone|Alexander Graham Bell is credited with inventing the telephone (patent 1876).",
            "who invented light bulb; light bulb|Thomas Edison developed the first commercially practical light bulb (1879), building on earlier work by others.",
            "who invented airplane; wright brothers|The Wright brothers, Orville and Wilbur, made the first powered flight in 1903.",
            "who is albert einstein; einstein|Albert Einstein was a physicist who developed the theory of relativity and the equation E = mc^2. He won the 1921 Nobel Prize in Physics.",
            "who is isaac newton; newton|Isaac Newton was an English scientist who formulated the laws of motion and universal gravitation.",
            "who is marie curie; marie curie|Marie Curie pioneered research on radioactivity and won Nobel Prizes in both Physics and Chemistry.",
            "who is shakespeare; william shakespeare|William Shakespeare was an English playwright. His works include Hamlet, Macbeth and Romeo and Juliet.",
            // ---- COMMERCE AND FINANCE ----
            "what is accounting|Accounting is the process of recording, classifying, summarizing and reporting financial transactions of a business.",
            "what is journal; journal entry|A journal is the book of original entry where each transaction is first recorded in date order with its debit and credit.",
            "what is ledger|A ledger is the book where journal entries are grouped by account, such as cash account or sales account.",
            "what is trial balance|A trial balance lists all ledger balances to check that total debits equal total credits.",
            "what is balance sheet|A balance sheet shows a business's assets, liabilities and capital on a particular date. Assets = Liabilities + Capital.",
            "what is asset; what is liability|Assets are resources a business owns (cash, stock, machinery). Liabilities are what it owes to others (loans, creditors).",
            "golden rules of accounting; rules of debit and credit|Personal: debit the receiver, credit the giver. Real: debit what comes in, credit what goes out. Nominal: debit expenses and losses, credit incomes and gains.",
            "what is depreciation|Depreciation is the fall in value of a fixed asset due to use or time. Straight-line: (Cost - Scrap value) / Useful life.",
            "what is gst; goods and services tax|GST (Goods and Services Tax) is an indirect tax on the supply of goods and services in India, introduced on 1 July 2017. It replaced many earlier taxes.",
            "what is stock market; what is share|A share is a unit of ownership in a company. The stock market is where shares are bought and sold, e.g. NSE and BSE in India.",
            "what is sebi|SEBI (Securities and Exchange Board of India) regulates the securities market and protects investors.",
            "what is sensex; what is nifty|Sensex is the index of 30 large companies on the BSE. Nifty 50 is the index of 50 large companies on the NSE.",
            "what is inflation|Inflation is the general rise in prices over time, which reduces the buying power of money.",
            "what is gdp; gross domestic product|GDP (Gross Domestic Product) is the total value of goods and services produced within a country in a year.",
            "what is rbi; reserve bank of india|The RBI (Reserve Bank of India) is India's central bank. It was established in 1935 and controls money supply, interest rates and currency.",
            "what is mutual fund|A mutual fund pools money from many investors and invests it in shares, bonds or other assets managed by professionals.",
            "what is sip; systematic investment plan|A SIP (Systematic Investment Plan) lets you invest a fixed amount regularly (monthly) in a mutual fund.",
            "what is demand and supply; law of demand|Law of demand: when price rises, quantity demanded falls. Law of supply: when price rises, quantity supplied rises. The price settles where they meet.",
            "what is management|Management is the process of planning, organizing, directing and controlling resources to achieve goals efficiently.",
            "what is entrepreneurship; who is entrepreneur|An entrepreneur starts and runs a business, taking risks to turn an idea into a product or service.",
            "how to calculate profit; profit formula|Profit = Revenue - Expenses. Profit % = (Profit / Cost price) x 100."
        };

        Chatbot() {
            for (String line : DATA) {
                int bar = line.indexOf('|');
                addFact(line.substring(0, bar), line.substring(bar + 1));
            }
            loadMemory();
            rebuild();
        }

        // ---------- build knowledge ----------
        private void addFact(String phrases, String answer) {
            Fact f = new Fact();
            f.answer = answer;
            for (String p : phrases.split(";")) {
                p = p.trim();
                if (p.isEmpty()) continue;
                if (f.shown == null) f.shown = p;
                Set<String> t = tokens(p);
                if (!t.isEmpty()) f.forms.add(t);
            }
            if (f.shown != null && !f.forms.isEmpty()) kb.add(f);
        }

        private void rebuild() {
            Map<String, Integer> df = new HashMap<>();
            for (Fact f : kb) {
                Set<String> seen = new HashSet<>();
                for (Set<String> form : f.forms) seen.addAll(form);
                for (String t : seen) df.merge(t, 1, Integer::sum);
            }
            idf.clear();
            maxIdf = 1;
            for (Map.Entry<String, Integer> e : df.entrySet()) {
                double v = Math.log(1.0 + (double) kb.size() / e.getValue());
                idf.put(e.getKey(), v);
                maxIdf = Math.max(maxIdf, v);
            }
        }

        private void loadMemory() {
            try {
                if (!Files.exists(memFile)) return;
                for (String line : Files.readAllLines(memFile)) {
                    int bar = line.indexOf('|');
                    if (bar > 0) addFact(line.substring(0, bar), line.substring(bar + 1));
                }
            } catch (Exception ignored) { }
        }

        private void saveMemory(String q, String a) {
            try {
                Files.write(memFile, Arrays.asList(q + "|" + a),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) { }   // some online compilers do not allow files
        }

        // ---------- text processing ----------
        private String clean(String t) {
            return t.toLowerCase().replace("c++", "cpp").replace("c#", "csharp").replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        }

        private String stem(String w) {
            if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us") && !w.endsWith("is"))
                return w.substring(0, w.length() - 1);
            return w;
        }

        private Set<String> tokens(String text) {
            Set<String> out = new LinkedHashSet<>();
            for (String w : clean(text).split(" ")) {
                if (w.isEmpty() || STOP.contains(w)) continue;
                w = stem(w);
                out.add(SYN.getOrDefault(w, w));
            }
            return out;
        }

        // Fix small spelling mistakes by comparing with known words
        private String correct(String w) {
            if (idf.containsKey(w) || w.length() <= 3 || w.matches(".*\\d.*")) return w;
            int limit = w.length() >= 11 ? 3 : (w.length() >= 8 ? 2 : 1);
            String best = w;
            int bestDist = limit + 1;
            for (String v : idf.keySet()) {
                if (Math.abs(v.length() - w.length()) > limit) continue;
                int d = distance(w, v);
                if (d < bestDist) { bestDist = d; best = v; }
            }
            return best;
        }

        // edit distance that counts swapping two neighbouring letters as one mistake
        private int distance(String a, String b) {
            int[][] d = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i <= a.length(); i++) d[i][0] = i;
            for (int j = 0; j <= b.length(); j++) d[0][j] = j;
            for (int i = 1; i <= a.length(); i++) {
                for (int j = 1; j <= b.length(); j++) {
                    int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                    d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                    if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
                        d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
            return d[a.length()][b.length()];
        }

        // ---------- main entry ----------
        String getResponse(String message) {
            String m = clean(message);
            if (m.isEmpty()) return "Please type a question or a message.";

            // 1. teach the bot:  teach: question = answer
            Matcher teach = Pattern.compile("(?i)^\\s*(?:teach|learn)\\s*:?\\s*(.+?)\\s*(?:=>|->|=)\\s*(.+)$").matcher(message);
            if (teach.matches()) {
                String q = teach.group(1).replace("|", "/").replace(";", ",");
                String a = teach.group(2).replace("|", "/");
                int before = kb.size();
                addFact(q, a);
                if (kb.size() == before) return "I couldn't learn that. Try: teach: what is zorb = Zorb is a made-up word";
                rebuild();
                saveMemory(q, a);
                return "Thanks! I've learned that. Now try asking me: " + q;
            }

            // 2. user's name
            Matcher nm = Pattern.compile("(?:my name is|call me|i am called)\\s+([a-z]+)").matcher(m);
            if (nm.find()) {
                String n = nm.group(1);
                userName = Character.toUpperCase(n.charAt(0)) + n.substring(1);
                return "Nice to meet you, " + userName + "!";
            }
            if (m.equals("what is my name") || m.equals("who am i") || m.equals("do you know my name"))
                return userName != null ? "Your name is " + userName + "." : "I don't know yet. Tell me: my name is ...";

            // 3. calculator
            String math = tryMath(message);
            if (math != null) return math;

            // 4. small talk and utilities
            String talk = smallTalk(m);
            if (talk != null) return talk;

            // 5. knowledge search
            return search(m);
        }

        private String smallTalk(String m) {
            if (m.matches("(hi|hii|hello|hey|namaste|yo|hola)( there)?") || m.startsWith("good morning")
                    || m.startsWith("good afternoon") || m.startsWith("good evening"))
                return "Hello" + (userName != null ? ", " + userName : "") + "! What would you like to know?";
            if (m.contains("how are you") || m.equals("how r u") || m.equals("whats up") || m.equals("what s up"))
                return "I'm doing great, thanks for asking! What would you like to know?";
            if (m.contains("thank")) return "You're welcome! Ask me anything else.";
            if (m.equals("bye") || m.contains("goodbye") || m.equals("see you") || m.equals("good night"))
                return "Goodbye" + (userName != null ? ", " + userName : "") + "! Have a great day.";
            if (m.contains("your name") || m.equals("who are you") || m.contains("introduce yourself") || m.contains("about yourself"))
                return "I'm a Java chatbot. I use keyword scoring with spelling correction to find the best answer from my knowledge base of " + kb.size() + " topics.";
            if (m.equals("help") || m.equals("topics") || m.contains("what can you do"))
                return "I can: (1) answer questions on technology, science, maths, geography, India, history and commerce; "
                        + "(2) calculate: '25% of 480', '(12+8)*3', 'sqrt 144', '2^10'; (3) tell the time/date, jokes and fun facts; "
                        + "(4) remember your name ('my name is ...'); (5) learn new things: 'teach: question = answer'.";
            if (m.contains("joke")) return JOKES[rnd.nextInt(JOKES.length)];
            if (m.contains("fun fact") || m.contains("tell me a fact") || m.equals("fact") || m.contains("interesting fact"))
                return FUN[rnd.nextInt(FUN.length)];
            LocalDateTime now = LocalDateTime.now();
            if (m.contains("what time") || m.equals("time") || m.contains("current time"))
                return "It is " + now.format(DateTimeFormatter.ofPattern("hh:mm a")) + " on the clock of the device I'm running on.";
            if (m.contains("what day") || m.contains("which day"))
                return "Today is " + now.format(DateTimeFormatter.ofPattern("EEEE")) + ".";
            if (m.contains("date") && (m.contains("today") || m.contains("current") || m.startsWith("what is the date") || m.startsWith("what date")))
                return "Today's date is " + now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + ".";
            if (m.contains("what year") || m.equals("current year"))
                return "It is " + now.getYear() + ".";
            return null;
        }

        private String search(String m) {
            List<String> q = new ArrayList<>();
            List<String> unknown = new ArrayList<>();
            for (String t : tokens(m)) {
                String c = correct(t);
                q.add(c);
                if (!idf.containsKey(c)) unknown.add(c);
            }
            if (q.isEmpty()) return fallback(unknown);

            double qTotal = 0;
            for (String t : q) qTotal += idf.containsKey(t) ? idf.get(t) : 0.5 * maxIdf;

            Fact best = null;
            double bestScore = 0, bestCover = 0;
            for (Fact f : kb) {
                for (Set<String> form : f.forms) {
                    double matched = 0, formTotal = 0;
                    for (String t : form) formTotal += idf.get(t);
                    for (String t : q) if (form.contains(t)) matched += idf.get(t);
                    if (matched == 0) continue;
                    double score = 0.65 * (matched / qTotal) + 0.35 * (matched / formTotal);
                    if (score > bestScore) { bestScore = score; best = f; bestCover = matched / qTotal; }
                }
            }
            if (best != null && bestScore >= 0.55 && bestCover >= 0.6) return best.answer;
            // a word I have never seen means I probably don't know the answer: don't guess
            if (best != null && bestScore >= 0.35 && unknown.isEmpty() && bestCover >= 0.4)
                return "I'm not fully sure, but I think you're asking about \"" + best.shown + "\": " + best.answer;
            return fallback(unknown);
        }

        private String fallback(List<String> unknown) {
            Fact a = kb.get(rnd.nextInt(kb.size())), b = kb.get(rnd.nextInt(kb.size()));
            String start = unknown.isEmpty() ? "I don't know that yet. "
                    : "I don't know about \"" + String.join(" ", unknown) + "\" yet. ";
            return start + "You can teach me with: teach: your question = the answer. "
                    + "Or try asking something like \"" + a.shown + "\" or \"" + b.shown + "\".";
        }

        // ---------- calculator ----------
        private String tryMath(String raw) {
            String s = raw.toLowerCase().trim().replace("?", "").replace("=", "").trim();
            for (int i = 0; i < 2; i++)
                s = s.replaceFirst("^(what is|what's|whats|how much is|calculate|compute|solve|evaluate|find)\\s+", "");
            s = s.replaceAll("\\bsquare root of\\s*", "sqrt ").replaceAll("\\bsqrt of\\s*", "sqrt ")
                    .replaceAll("\\bplus\\b", "+").replaceAll("\\bminus\\b", "-")
                    .replaceAll("\\b(times|multiplied by)\\b", "*").replaceAll("\\b(divided by|over)\\b", "/")
                    .replaceAll("\\bto the power of\\b|\\bpower\\b", "^")
                    .replaceAll("(?<=\\d)\\s*x\\s*(?=\\d)", "*");

            Matcher pm = Pattern.compile("^([0-9.]+)\\s*(?:%|percent)\\s*of\\s*([0-9.]+)$").matcher(s);
            if (pm.matches()) {
                try {
                    return pm.group(1) + "% of " + pm.group(2) + " = "
                            + fmt(Double.parseDouble(pm.group(1)) * Double.parseDouble(pm.group(2)) / 100);
                } catch (NumberFormatException e) { return "That doesn't look like a valid number."; }
            }

            String test = s.replace("sqrt", "");
            if (!test.matches("[0-9+\\-*/^().\\s]+") || !test.matches(".*\\d.*")) return null;
            if (!(s.matches(".*[+*/^].*") || s.contains("sqrt") || s.matches(".*\\d\\s*-\\s*[\\d(].*"))) return null;
            try {
                return s.trim() + " = " + fmt(new Calc(s.replaceAll("\\s+", "")).parse());
            } catch (ArithmeticException | NumberFormatException e) {
                String why = e.getMessage();
                return why != null && why.startsWith("Cannot") ? why : "I couldn't calculate that. Please check the expression.";
            }
        }

        private String fmt(double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new ArithmeticException("Cannot show that result.");
            if (Math.abs(v - Math.rint(v)) < 1e-9 && Math.abs(v) < 1e15) return String.valueOf((long) Math.rint(v));
            String r = String.format("%.6f", v);
            return r.replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        // small recursive-descent expression parser: + - * / ^ ( ) sqrt
        static class Calc {
            private final String s;
            private int p = 0;
            Calc(String s) { this.s = s; }

            double parse() {
                double v = expr();
                if (p < s.length()) throw new ArithmeticException("bad");
                return v;
            }
            private boolean at(char c) { return p < s.length() && s.charAt(p) == c; }
            private double expr() {
                double v = term();
                while (at('+') || at('-')) {
                    char c = s.charAt(p++);
                    double r = term();
                    v = c == '+' ? v + r : v - r;
                }
                return v;
            }
            private double term() {
                double v = unary();
                while (at('*') || at('/')) {
                    char c = s.charAt(p++);
                    double r = unary();
                    if (c == '/') {
                        if (r == 0) throw new ArithmeticException("Cannot divide by zero.");
                        v /= r;
                    } else v *= r;
                }
                return v;
            }
            private double unary() {
                if (at('-')) { p++; return -unary(); }
                if (at('+')) { p++; return unary(); }
                return power();
            }
            private double power() {
                double b = primary();
                if (at('^')) { p++; return Math.pow(b, unary()); }
                return b;
            }
            private double primary() {
                if (s.startsWith("sqrt", p)) {
                    p += 4;
                    double v = unary();
                    if (v < 0) throw new ArithmeticException("Cannot take the square root of a negative number.");
                    return Math.sqrt(v);
                }
                if (at('(')) {
                    p++;
                    double v = expr();
                    if (!at(')')) throw new ArithmeticException("bad");
                    p++;
                    return v;
                }
                int start = p;
                while (p < s.length() && (Character.isDigit(s.charAt(p)) || s.charAt(p) == '.')) p++;
                if (start == p) throw new ArithmeticException("bad");
                return Double.parseDouble(s.substring(start, p));
            }
        }
    }
}
