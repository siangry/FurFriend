package com.example.furfriend.screen.profile;

public class Pet {
    private String name;
    private String type;
    private int age;
    private String ageUnit;
    private String gender;
    private String petImage;

    public Pet() {
    }

    public Pet(String name, String type, int age, String ageUnit, String gender, String petImage) {
        this.name = name;
        this.type = type;
        this.age = age;
        this.ageUnit = ageUnit;
        this.gender = gender;
        this.petImage = petImage;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getAge() {
        return age;
    }

    public String getAgeUnit() {
        return ageUnit;
    }

    public String getGender() {
        return gender;
    }

    public String getPetImage() {
        return petImage;
    }
}
