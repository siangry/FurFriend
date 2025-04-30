package com.example.furfriend.screen.profile;

public class Pet {
    private String name;
    private String type;
    private int age;
    private String ageUnit;
    private String gender;
    private String petImage;
    private String weight;
    private String petId;

    public Pet() {
    }

    public Pet(String name, String type, int age, String ageUnit, String gender, String petImage, String weight, String petId) {
        this.name = name;
        this.type = type;
        this.age = age;
        this.ageUnit = ageUnit;
        this.gender = gender;
        this.petImage = petImage;
        this.weight = weight;
        this.petId = petId;
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

    public String getWeight() {
        return weight;
    }

    public String getPetId() {
        return petId;
    }
}
