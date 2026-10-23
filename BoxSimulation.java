import java.util.*;

// คลาส Box ใช้เก็บข้อมูลของกล่องแต่ละใบ
class Box {
    String id;       // รหัสประจำกล่อง
    int weight;      // น้ำหนักของกล่อง

    // Constructor สำหรับกำหนดรหัสและน้ำหนักตอนสร้างกล่อง
    public Box(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }
}

// คลาสหลักสำหรับจำลองการค้นหาและย้ายกล่องระหว่าง Stack
public class BoxSimulation {
    // เก็บ Stack แต่ละกอง โดยใช้ชื่อ Stack เป็น key
    private Map<String, Stack<Box>> stacks;

    // เก็บความจุสูงสุดของแต่ละ Stack
    private Map<String, Integer> capacities;

    // ตัวแปรสถิติสำหรับนับจำนวนกล่องที่มีการย้าย
    private int boxesMoved = 0;

    // ตัวแปรนับจำนวนครั้งที่ตรวจสอบ Stack
    private int stackChecks = 0;

    // ตัวแปรนับจำนวนกรณีที่ทำงานไม่สำเร็จ
    private int failures = 0;

    // เก็บเวลารวมที่ใช้ในการทำงาน หน่วยเป็นมิลลิวินาที
    private double executionTime = 0.0;

    // Constructor รับความจุของแต่ละ Stack และสร้าง Stack ว่างตามชื่อที่กำหนด
    public BoxSimulation(Map<String, Integer> capacities) {
        this.capacities = capacities;
        this.stacks = new HashMap<>();

        // สร้าง Stack ว่างให้ทุกชื่อที่อยู่ใน capacities
        for (String stackName : capacities.keySet()) {
            this.stacks.put(stackName, new Stack<>());
        }
    }

    // เพิ่มกล่องเข้า Stack ที่ระบุ
    public void pushToStack(String stackName, Box box) {
        stacks.get(stackName).push(box);
    }

    // 1. ค้นหากล่องเป้าหมายจากทุก Stack
    // แนวคิด: ตรวจสอบกล่องจากด้านบนลงล่าง จนกว่าจะพบ ID ที่ต้องการ
    // ในกรณีทั่วไป ความซับซ้อนขึ้นอยู่กับจำนวนกล่องทั้งหมด คือ O(n)
    public TargetResult findTargetBox(String targetId) {
        stackChecks++; // นับการเรียกค้นหา

        // วนตรวจสอบ Stack ทุกกอง
        for (Map.Entry<String, Stack<Box>> entry : stacks.entrySet()) {
            String stackName = entry.getKey();
            Stack<Box> stack = entry.getValue();

            // คัดลอกกล่องใน Stack มาเป็น List เพื่อไล่ตรวจจากด้านบนลงล่าง
            List<Box> list = new ArrayList<>(stack);

            for (int i = list.size() - 1; i >= 0; i--) {
                // ถ้า ID ตรงกับกล่องเป้าหมาย ให้คืนข้อมูลตำแหน่ง
                if (list.get(i).id.equals(targetId)) {
                    int h = list.size() - 1 - i; // จำนวนกล่องที่อยู่เหนือเป้าหมาย
                    return new TargetResult(stackName, i, h);
                }
            }
        }

        // คืนค่า null เมื่อไม่พบกล่องเป้าหมาย
        return null;
    }

    // 2. เลือก Stack ปลายทางแบบ First Available
    // เลือก Stack แรกที่มีพื้นที่ว่างเพียงพอ
    public String selectDestinationFirstAvailable(int requiredSpace) {
        for (Map.Entry<String, Stack<Box>> entry : stacks.entrySet()) {
            stackChecks++; // นับการตรวจสอบ Stack
            String stackName = entry.getKey();

            // คำนวณพื้นที่ว่าง = ความจุสูงสุด - จำนวนกล่องปัจจุบัน
            int freeSpace = capacities.get(stackName) - entry.getValue().size();

            // ถ้ามีพื้นที่เพียงพอ ให้เลือก Stack นี้ทันที
            if (freeSpace >= requiredSpace) {
                return stackName;
            }
        }

        // ไม่มี Stack ใดมีพื้นที่เพียงพอ
        return null;
    }

    // 2. เลือก Stack ปลายทางแบบ Best Fit
    // เลือก Stack ที่เมื่อวางกล่องแล้วจะเหลือพื้นที่ว่างน้อยที่สุด
    public String selectDestinationBestFit(int requiredSpace) {
        String bestStack = null;
        int minRemainingSpace = Integer.MAX_VALUE;

        for (Map.Entry<String, Stack<Box>> entry : stacks.entrySet()) {
            stackChecks++; // นับการตรวจสอบ Stack
            String stackName = entry.getKey();
            int freeSpace = capacities.get(stackName) - entry.getValue().size();

            // พิจารณาเฉพาะ Stack ที่มีพื้นที่เพียงพอ
            if (freeSpace >= requiredSpace) {
                int remaining = freeSpace - requiredSpace;

                // ถ้าเหลือพื้นที่น้อยกว่าตัวเลือกก่อนหน้า ให้เลือก Stack นี้
                if (remaining < minRemainingSpace) {
                    minRemainingSpace = remaining;
                    bestStack = stackName;
                }
            }
        }

        // คืนชื่อ Stack ที่เหมาะสมที่สุด หรือ null หากไม่มีพื้นที่
        return bestStack;
    }

    // 3. และ 4. จำลองการย้ายกล่องที่อยู่เหนือเป้าหมายออกชั่วคราว
    // จากนั้นนำกล่องเป้าหมายออก แล้วคืนกล่องที่ย้ายไว้กลับเข้าที่
    public boolean executeMove(String targetId, String algorithm) {
        long startTime = System.nanoTime(); // บันทึกเวลาเริ่มต้น

        // ค้นหาตำแหน่งของกล่องเป้าหมายก่อนเริ่มย้าย
        TargetResult target = findTargetBox(targetId);

        if (target == null) {
            failures++;
            System.out.println("Test Case Failed: ไม่พบกล่องเป้าหมาย " + targetId);
            return false;
        }

        // อ้างอิง Stack ที่พบกล่องเป้าหมาย
        Stack<Box> sourceStack = stacks.get(target.stackName);
        List<Box> boxesAbove = new ArrayList<>();

        // ดึงกล่องที่อยู่เหนือเป้าหมายออกจาก Stack ต้นทางชั่วคราว
        // กล่องที่ถูก pop จะถูกเก็บใน boxesAbove
        while (sourceStack.size() - 1 > target.index) {
            boxesAbove.add(sourceStack.pop());
        }

        // เก็บข้อมูลว่าแต่ละกล่องถูกย้ายไป Stack ใด เพื่อใช้คืนภายหลัง
        List<MoveRecord> temporaryStorage = new ArrayList<>();

        // ย้ายกล่องที่อยู่เหนือเป้าหมายทีละใบไปยัง Stack ปลายทาง
        for (Box box : boxesAbove) {
            // เลือกวิธีจัดเก็บตามค่า algorithm ที่ส่งเข้ามา
            String dest = algorithm.equals("first_available")
                    ? selectDestinationFirstAvailable(1)
                    : selectDestinationBestFit(1);

            // ถ้าไม่มีปลายทาง หรือเลือก Stack ต้นทางเอง ให้แจ้งว่าล้มเหลว
            if (dest == null || dest.equals(target.stackName)) {
                failures++;
                System.out.println("Test Case Failed: ไม่มี Stack ปลายทางเพียงพอ");
                return false;
            }

            // นำกล่องไปวางบน Stack ปลายทางและบันทึกประวัติการย้าย
            stacks.get(dest).push(box);
            temporaryStorage.add(new MoveRecord(dest, box));
            boxesMoved++;
        }

        // เมื่อกล่องด้านบนถูกนำออกแล้ว จึงหยิบกล่องเป้าหมายออกได้
        Box targetBox = sourceStack.pop();

        // คืนกล่องที่ย้ายชั่วคราวกลับไปยัง Stack ต้นทางตามลำดับย้อนกลับ
        for (int i = temporaryStorage.size() - 1; i >= 0; i--) {
            MoveRecord record = temporaryStorage.get(i);

            // นำกล่องออกจาก Stack ปลายทาง แล้ววางกลับใน Stack ต้นทาง
            stacks.get(record.dest).pop();
            sourceStack.push(record.box);
            boxesMoved++;
        }

        // วางกล่องเป้าหมายกลับคืนด้านบนของ Stack ต้นทาง
        sourceStack.push(targetBox);

        long endTime = System.nanoTime(); // บันทึกเวลาสิ้นสุด
        executionTime += (endTime - startTime) / 1_000_000.0; // แปลงนาโนวินาทีเป็นมิลลิวินาที

        return true; // ทำงานสำเร็จ
    }

    // แสดงจำนวนช่องว่างที่เหลือในแต่ละ Stack
    public void printRemainingSpace() {
        System.out.println("--- พื้นที่ว่างหลังการย้าย ---");

        for (String stackName : stacks.keySet()) {
            int free = capacities.get(stackName) - stacks.get(stackName).size();
            System.out.println("Stack " + stackName + " เหลือพื้นที่ว่าง: " + free + " ช่อง");
        }
    }

    // คลาสช่วยเก็บผลการค้นหากล่องเป้าหมาย
    public static class TargetResult {
        String stackName; // ชื่อ Stack ที่พบกล่อง
        int index;        // ตำแหน่งของกล่องใน List โดยเริ่มจาก 0
        int h;             // จำนวนกล่องที่อยู่เหนือกล่องเป้าหมาย

        public TargetResult(String stackName, int index, int h) {
            this.stackName = stackName;
            this.index = index;
            this.h = h;
        }
    }

    // คลาสช่วยบันทึกว่ากล่องใดถูกย้ายไป Stack ปลายทางใด
    public static class MoveRecord {
        String dest; // ชื่อ Stack ปลายทาง
        Box box;     // กล่องที่ถูกย้าย

        public MoveRecord(String dest, Box box) {
            this.dest = dest;
            this.box = box;
        }
    }

    // จุดเริ่มต้นของโปรแกรม
    public static void main(String[] args) {
        // กำหนดความจุของ Stack A, B และ C ให้รองรับได้กองละ 5 กล่อง
        Map<String, Integer> capacities = new LinkedHashMap<>();
        capacities.put("A", 5);
        capacities.put("B", 5);
        capacities.put("C", 5);

        // สร้างออบเจ็กต์จำลองการจัดเก็บกล่อง
        BoxSimulation sim = new BoxSimulation(capacities);

        // เพิ่มกล่อง 3 ใบลงใน Stack A โดย Box3 อยู่ด้านบนสุด
        sim.pushToStack("A", new Box("Box1", 10));
        sim.pushToStack("A", new Box("Box2", 20));
        sim.pushToStack("A", new Box("Box3", 15)); // กล่องเป้าหมาย

        // ทดลองค้นหาและย้ายกล่อง Box3 ด้วยอัลกอริทึม First Available
        boolean success = sim.executeMove("Box3", "first_available");

        // แสดงผลเมื่อการทำงานสำเร็จ
        if (success) {
            System.out.println("ย้ายสำเร็จ!");
            sim.printRemainingSpace();
        }
    }
}
