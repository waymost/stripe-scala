package com.stripe

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.UUID

trait StripeSuite extends ShouldMatchers {
  //set the stripe API key
  apiKey = System.getProperty("apiKey")

  val DefaultCardMap = Map(
    "name" -> "Scala User",
    "cvc" -> "100",
    "address_line1" -> "12 Main Street",
    "address_line2" -> "Palo Alto",
    "address_country" -> "USA",
    "number" -> "4242424242424242",
    "exp_month" -> 3,
    "exp_year" -> 2015)

  val DefaultChargeMap = Map("amount" -> 100, "currency" -> "usd", "card" -> DefaultCardMap)

  val DefaultCustomerMap = Map("description" -> "Scala Customer", "card" -> DefaultCardMap)

  val DefaultPlanMap = Map("amount" -> 100, "currency" -> "usd", "interval" -> "month", "name" -> "Scala Plan")

  def getUniquePlanId(): String = return "PLAN-%s".format(UUID.randomUUID())

  def getUniquePlanMap(): Map[String,_] = return DefaultPlanMap + ("id" -> getUniquePlanId())
}

class ChargeSuite extends FunSuite with StripeSuite {
  test("Charges can be created") {
    val charge = Charge.create(Map("amount" -> 100, "currency" -> "usd", "card" -> DefaultCardMap))
    charge.refunded should be (false)
  }

  test("Charges can be retrieved") {
    val createdCharge = Charge.create(DefaultChargeMap)
    val retrievedCharge = Charge.retrieve(createdCharge.id)
    createdCharge.created should equal (retrievedCharge.created)
  }

  test("Charges can be refunded") {
    val charge = Charge.create(DefaultChargeMap)
    val refundedCharge = charge.refund()
    refundedCharge.refunded should equal (true)
  }

  test("Charges can be listed") {
    val charge = Charge.create(DefaultChargeMap)
    val charges = Charge.all()
    charges.head.isInstanceOf[Charge] should be (true)
  }

  test("Invalid card raises CardException") {
    val e = intercept[CardException] {
      Charge.create(Map(
        "amount" -> 100,
        "currency" -> "usd",
        "card" -> Map("number" -> "4242424242424241", "exp_month" -> 3, "exp_year" -> 2015)
      ))
    }
    e.param.get should equal ("number")
  }
}

class CustomerSuite extends FunSuite with StripeSuite {
  test("Customers can be created") {
    val customer = Customer.create(DefaultCustomerMap + ("description" -> "Test Description"))
    customer.description.get should be ("Test Description")
    customer.activeCard.isEmpty should be (false)
  }

  test("Customers can be retrieved") {
    val createdCustomer = Customer.create(DefaultCustomerMap)
    val retrievedCustomer = Customer.retrieve(createdCustomer.id)
    createdCustomer.created should equal (retrievedCustomer.created)
  }

  test("Customers can be updated") {
    val customer = Customer.create(DefaultCustomerMap)
    val updatedCustomer = customer.update(Map("description" -> "Updated Scala Customer"))
    updatedCustomer.description.get should equal ("Updated Scala Customer")
  }

  test("Customers can be deleted") {
    val customer = Customer.create(DefaultCustomerMap)
    val deletedCustomer = customer.delete()
    deletedCustomer.deleted should be (true)
    deletedCustomer.id should equal (customer.id)
  }

  test("Customers can be listed") {
    val customer = Customer.create(DefaultCustomerMap)
    val customers = Customer.all()
    customers.head.isInstanceOf[Customer] should be (true)
  }
}

class PlanSuite extends FunSuite with StripeSuite {
  test("Plans can be created") {
    val plan = Plan.create(getUniquePlanMap)
    plan.`object` should equal ("plan")
  }

  test("Plans can be retrieved") {
    val createdPlan = Plan.create(getUniquePlanMap)
    val retrievedPlan = Plan.retrieve(createdPlan.id)
    createdPlan should equal (retrievedPlan)
  }

  test("Plans can be deleted") {
    val plan = Plan.create(getUniquePlanMap)
    val deletedPlan = plan.delete()
    deletedPlan.deleted should be (true)
    deletedPlan.id should equal (plan.id)
  }

  test("Plans can be listed") {
    val plan = Plan.create(getUniquePlanMap)
    val plans = Plan.all()
    plans.head.isInstanceOf[Plan] should be (true)
  }

  test("Customers can be created with a plan") {
    val plan = Plan.create(getUniquePlanMap)
    val customer = Customer.create(DefaultCustomerMap + ("plan" -> plan.id))
    customer.subscription.get.plan.id should equal (plan.id)
  }

  test("A plan can be added to a customer without a plan") {
    val customer = Customer.create(DefaultCustomerMap)
    val plan = Plan.create(getUniquePlanMap)
    val subscription = customer.updateSubscription(Map("plan" -> plan.id))
    subscription.customer should equal (customer.id)
    subscription.plan.id should equal (plan.id)
  }

  test("A customer's existing plan can be replaced") {
    val origPlan = Plan.create(getUniquePlanMap)
    val customer = Customer.create(DefaultCustomerMap + ("plan" -> origPlan.id))
    customer.subscription.get.plan.id should equal (origPlan.id)
    val newPlan = Plan.create(getUniquePlanMap)
    val subscription = customer.updateSubscription(Map("plan" -> newPlan.id))
    val updatedCustomer = Customer.retrieve(customer.id)
    updatedCustomer.subscription.get.plan.id should equal (newPlan.id)
  }

  test("Customer subscriptions can be canceled") {
    val plan = Plan.create(getUniquePlanMap)
    val customer = Customer.create(DefaultCustomerMap + ("plan" -> plan.id))
    customer.subscription.get.status should equal ("active")
    val canceledSubscription = customer.cancelSubscription()
    canceledSubscription.status should be ("canceled")
  }
}